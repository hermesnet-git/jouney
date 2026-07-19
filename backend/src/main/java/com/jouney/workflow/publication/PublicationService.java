package com.jouney.workflow.publication;

import com.jouney.workflow.audit.AuditAction;
import com.jouney.workflow.audit.AuditService;
import com.jouney.workflow.definition.WorkflowDefinition;
import com.jouney.workflow.definition.WorkflowDefinitionService;
import com.jouney.workflow.shared.error.ValidationFailedException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** FR-004/FR-006: valida antes de publicar; publica uma versão imutável e a torna a ativa. */
@Service
public class PublicationService {

  private final WorkflowDefinitionService definitionService;
  private final WorkflowVersionRepository versionRepository;
  private final AuditService auditService;

  public PublicationService(
      WorkflowDefinitionService definitionService,
      WorkflowVersionRepository versionRepository,
      AuditService auditService) {
    this.definitionService = definitionService;
    this.versionRepository = versionRepository;
    this.auditService = auditService;
  }

  @Transactional
  public WorkflowVersion publish(UUID workflowDefinitionId, String publishedBy) {
    WorkflowDefinition definition = definitionService.get(workflowDefinitionId);

    List<String> problems = definitionService.validateGraphJson(definition.getGraphJson());
    if (!problems.isEmpty()) {
      throw new ValidationFailedException(problems);
    }

    versionRepository
        .findByWorkflowDefinitionIdAndActiveTrue(workflowDefinitionId)
        .ifPresent(WorkflowVersion::deactivate);

    int nextVersionNumber = versionRepository.countByWorkflowDefinitionId(workflowDefinitionId) + 1;
    WorkflowVersion version =
        new WorkflowVersion(
            workflowDefinitionId, nextVersionNumber, definition.getGraphJson(), publishedBy);
    versionRepository.save(version);

    definition.markPublished();

    auditService.record(
        publishedBy,
        AuditAction.WORKFLOW_PUBLISHED,
        "WORKFLOW_VERSION",
        version.getId().toString(),
        java.util.Map.of(
            "workflowDefinitionId",
            workflowDefinitionId.toString(),
            "versionNumber",
            nextVersionNumber));

    return version;
  }

  public List<WorkflowVersion> listVersions(UUID workflowDefinitionId) {
    return versionRepository.findByWorkflowDefinitionIdOrderByVersionNumberDesc(
        workflowDefinitionId);
  }
}
