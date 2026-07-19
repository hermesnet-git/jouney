package com.jouney.workflow.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jouney.workflow.definition.WorkflowGraph.WorkflowEdge;
import com.jouney.workflow.definition.WorkflowGraph.WorkflowNode;
import com.jouney.workflow.expression.ExpressionEvaluator;
import com.jouney.workflow.shared.NodeType;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * T062 — avalia as condições dos caminhos de saída e segue o correspondente (FR-017); quando o
 * caminho escolhido tem {@code publishEvent}, grava um Outbox Event na mesma transação (FR-018,
 * research.md #6).
 */
@Component
public class ExclusiveGatewayNodeExecutor implements NodeExecutor {

  private final ExpressionEvaluator expressionEvaluator;
  private final OutboxEventRepository outboxEventRepository;
  private final ObjectMapper objectMapper;

  public ExclusiveGatewayNodeExecutor(
      ExpressionEvaluator expressionEvaluator,
      OutboxEventRepository outboxEventRepository,
      ObjectMapper objectMapper) {
    this.expressionEvaluator = expressionEvaluator;
    this.outboxEventRepository = outboxEventRepository;
    this.objectMapper = objectMapper;
  }

  @Override
  public boolean supports(String nodeType) {
    return NodeType.EXCLUSIVE_GATEWAY.name().equals(nodeType);
  }

  @Override
  public NodeExecutionResult execute(
      WorkflowNode node,
      WorkflowInstance instance,
      NodeInstance nodeInstance,
      ExecutionContext context,
      List<WorkflowEdge> outgoingEdges) {
    WorkflowEdge chosen =
        outgoingEdges.stream()
            .filter(e -> expressionEvaluator.evaluate(e.condition(), context.asMap()))
            .findFirst()
            .orElse(null);

    if (chosen == null) {
      return NodeExecutionResult.failed(
          "Nenhuma condição de '" + node.name() + "' corresponde aos dados da execução.",
          NodeExecutionResult.ErrorCategory.BUSINESS);
    }

    if (chosen.publishEvent() != null) {
      outboxEventRepository.save(
          new OutboxEvent("WORKFLOW_INSTANCE", instance.getId(), toJson(chosen.publishEvent())));
    }

    return NodeExecutionResult.completed(Map.of(), chosen.target());
  }

  private String toJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception e) {
      return "{}";
    }
  }
}
