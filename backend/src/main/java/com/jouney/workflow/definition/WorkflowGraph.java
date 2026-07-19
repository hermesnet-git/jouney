package com.jouney.workflow.definition;

import java.util.List;

/** Representação em memória do JSON nodes/edges (spec.md §5 / planejamento.md seção 5). */
public record WorkflowGraph(
    String workflowKey,
    String name,
    String startNodeId,
    List<WorkflowNode> nodes,
    List<WorkflowEdge> edges) {

  public record WorkflowNode(
      String id, String type, String name, java.util.Map<String, Object> configuration) {}

  public record WorkflowEdge(
      String id,
      String source,
      String target,
      String condition,
      java.util.Map<String, Object> publishEvent) {}
}
