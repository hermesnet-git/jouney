package com.jouney.workflow.runtime;

import com.jouney.workflow.audit.AuditAction;
import com.jouney.workflow.audit.AuditService;
import com.jouney.workflow.shared.error.NotFoundException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InstanceService {

  private final EngineDispatcher dispatcher;
  private final WorkflowInstanceRepository instanceRepository;
  private final NodeInstanceRepository nodeInstanceRepository;
  private final AuditService auditService;

  public InstanceService(
      EngineDispatcher dispatcher,
      WorkflowInstanceRepository instanceRepository,
      NodeInstanceRepository nodeInstanceRepository,
      AuditService auditService) {
    this.dispatcher = dispatcher;
    this.instanceRepository = instanceRepository;
    this.nodeInstanceRepository = nodeInstanceRepository;
    this.auditService = auditService;
  }

  /** T069/T071 — FR-020: cancela uma execução em andamento. */
  @Transactional
  public void cancel(UUID id) {
    WorkflowInstance instance = get(id);
    instance.cancel();
    instanceRepository.save(instance);
    auditService.record(
        currentActor(),
        AuditAction.EXECUTION_CANCELLED,
        "WORKFLOW_INSTANCE",
        id.toString(),
        Map.of());
  }

  /** T070/T071 — FR-020: reprocessa só a etapa que falhou, sem repetir etapas concluídas. */
  @Transactional
  public void retry(UUID id) {
    dispatcher.retryFailed(id);
    auditService.record(
        currentActor(),
        AuditAction.EXECUTION_RETRIED,
        "WORKFLOW_INSTANCE",
        id.toString(),
        Map.of());
  }

  private String currentActor() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    return auth != null ? auth.getName() : "system";
  }

  public WorkflowInstance start(UUID workflowVersionId, String businessKey, String startedBy) {
    return dispatcher.start(workflowVersionId, businessKey, startedBy);
  }

  public WorkflowInstance get(UUID id) {
    return instanceRepository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Instância " + id + " não encontrada."));
  }

  public List<WorkflowInstance> list() {
    return instanceRepository.findAllByOrderByStartedAtDesc();
  }

  public List<NodeInstance> history(UUID instanceId) {
    return nodeInstanceRepository.findByWorkflowInstanceIdOrderByStartedAtAsc(instanceId);
  }
}
