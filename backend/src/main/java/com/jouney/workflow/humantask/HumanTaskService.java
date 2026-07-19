package com.jouney.workflow.humantask;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jouney.workflow.audit.AuditAction;
import com.jouney.workflow.audit.AuditService;
import com.jouney.workflow.runtime.EngineDispatcher;
import com.jouney.workflow.runtime.NodeInstance;
import com.jouney.workflow.runtime.NodeInstanceRepository;
import com.jouney.workflow.shared.error.ConflictException;
import com.jouney.workflow.shared.error.NotFoundException;
import com.jouney.workflow.shared.error.ValidationFailedException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** T045-T047 — assumir, validar e concluir uma tarefa humana, retomando a execução (US3). */
@Service
public class HumanTaskService {

  private final HumanTaskRepository taskRepository;
  private final NodeInstanceRepository nodeInstanceRepository;
  private final FormValidator formValidator;
  private final EngineDispatcher dispatcher;
  private final AuditService auditService;
  private final ObjectMapper objectMapper;

  public HumanTaskService(
      HumanTaskRepository taskRepository,
      NodeInstanceRepository nodeInstanceRepository,
      FormValidator formValidator,
      EngineDispatcher dispatcher,
      AuditService auditService,
      ObjectMapper objectMapper) {
    this.taskRepository = taskRepository;
    this.nodeInstanceRepository = nodeInstanceRepository;
    this.formValidator = formValidator;
    this.dispatcher = dispatcher;
    this.auditService = auditService;
    this.objectMapper = objectMapper;
  }

  public List<HumanTask> listPending(String user, List<String> groups) {
    return taskRepository.findPendingForUser(user, groups.isEmpty() ? List.of("__none__") : groups);
  }

  public HumanTask get(UUID id) {
    return taskRepository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Tarefa " + id + " não encontrada."));
  }

  @Transactional
  public HumanTask claim(UUID id, String user) {
    HumanTask task = get(id);
    try {
      task.claim(user);
    } catch (IllegalStateException e) {
      throw new ConflictException("Esta tarefa já foi assumida por outra pessoa.");
    }
    return task;
  }

  @Transactional
  public void complete(UUID id, Map<String, Object> formData) {
    HumanTask task = get(id);
    FormSchema schema = parseSchema(task.getFormSchemaJson());

    List<String> problems = formValidator.validate(schema, formData);
    if (!problems.isEmpty()) {
      throw new ValidationFailedException(problems);
    }

    String formDataJson = toJson(formData);
    task.complete(formDataJson);

    NodeInstance nodeInstance =
        nodeInstanceRepository
            .findById(task.getNodeInstanceId())
            .orElseThrow(() -> new NotFoundException("Etapa da tarefa não encontrada."));
    nodeInstance.complete(formDataJson);
    nodeInstanceRepository.save(nodeInstance);

    auditService.record(
        task.getAssignee(), AuditAction.TASK_COMPLETED, "HUMAN_TASK", id.toString(), Map.of());

    dispatcher.resume(task.getWorkflowInstanceId());
  }

  private FormSchema parseSchema(String json) {
    try {
      return objectMapper.readValue(json, FormSchema.class);
    } catch (Exception e) {
      return new FormSchema("Formulário", List.of());
    }
  }

  private String toJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception e) {
      return "{}";
    }
  }
}
