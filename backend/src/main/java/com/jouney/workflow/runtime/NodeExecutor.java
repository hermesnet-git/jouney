package com.jouney.workflow.runtime;

import com.jouney.workflow.definition.WorkflowGraph.WorkflowEdge;
import com.jouney.workflow.definition.WorkflowGraph.WorkflowNode;
import java.util.List;

/**
 * Estratégia de execução por tipo de nó (mesma ideia do ConnectorExecutor da seção 10). Cada tipo
 * de etapa do MVP (T036, T043, T055, T062) ganha sua própria implementação, sem precisar alterar o
 * loop do {@link EngineDispatcher}. {@code outgoingEdges} é repassado só para os tipos que precisam
 * escolher entre múltiplos caminhos (T062, EXCLUSIVE_GATEWAY).
 */
public interface NodeExecutor {

  boolean supports(String nodeType);

  NodeExecutionResult execute(
      WorkflowNode node,
      WorkflowInstance instance,
      NodeInstance nodeInstance,
      ExecutionContext context,
      List<WorkflowEdge> outgoingEdges);
}
