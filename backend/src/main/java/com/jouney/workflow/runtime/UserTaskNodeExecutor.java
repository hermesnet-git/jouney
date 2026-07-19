package com.jouney.workflow.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jouney.workflow.definition.WorkflowGraph.WorkflowEdge;
import com.jouney.workflow.definition.WorkflowGraph.WorkflowNode;
import com.jouney.workflow.humantask.HumanTask;
import com.jouney.workflow.humantask.HumanTaskRepository;
import com.jouney.workflow.shared.NodeType;
import com.jouney.workflow.shared.WorkflowMetrics;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** T043 — cria a Human Task e coloca a instância em WAITING até ela ser concluída (US3). */
@Component
public class UserTaskNodeExecutor implements NodeExecutor {

  private final HumanTaskRepository humanTaskRepository;
  private final ObjectMapper objectMapper;
  private final WorkflowMetrics metrics;

  public UserTaskNodeExecutor(
      HumanTaskRepository humanTaskRepository, ObjectMapper objectMapper, WorkflowMetrics metrics) {
    this.humanTaskRepository = humanTaskRepository;
    this.objectMapper = objectMapper;
    this.metrics = metrics;
  }

  @Override
  public boolean supports(String nodeType) {
    return NodeType.USER_TASK.name().equals(nodeType);
  }

  @Override
  @SuppressWarnings("unchecked")
  public NodeExecutionResult execute(
      WorkflowNode node,
      WorkflowInstance instance,
      NodeInstance nodeInstance,
      ExecutionContext context,
      List<WorkflowEdge> outgoingEdges) {
    Map<String, Object> configuration =
        node.configuration() == null ? Map.of() : node.configuration();
    Map<String, Object> assignment =
        (Map<String, Object>) configuration.getOrDefault("assignment", Map.of());
    String assignmentType = String.valueOf(assignment.getOrDefault("type", "INITIATOR"));

    String assignee = "INITIATOR".equals(assignmentType) ? instance.getStartedBy() : null;
    String candidateGroup =
        "GROUP".equals(assignmentType) ? String.valueOf(assignment.get("group")) : null;

    Object formSchema = configuration.get("formSchema");
    String formSchemaJson =
        toJson(
            formSchema != null
                ? formSchema
                : Map.of("title", node.name(), "fields", java.util.List.of()));

    HumanTask task =
        new HumanTask(
            instance.getId(),
            nodeInstance.getId(),
            node.name(),
            assignee,
            candidateGroup,
            formSchemaJson);
    humanTaskRepository.save(task);
    metrics.pendingTask();

    return NodeExecutionResult.waiting();
  }

  private String toJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception e) {
      return "{}";
    }
  }
}
