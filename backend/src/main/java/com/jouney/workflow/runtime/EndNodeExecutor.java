package com.jouney.workflow.runtime;

import com.jouney.workflow.definition.WorkflowGraph.WorkflowEdge;
import com.jouney.workflow.definition.WorkflowGraph.WorkflowNode;
import com.jouney.workflow.shared.NodeType;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class EndNodeExecutor implements NodeExecutor {

  @Override
  public boolean supports(String nodeType) {
    return NodeType.END.name().equals(nodeType);
  }

  @Override
  public NodeExecutionResult execute(
      WorkflowNode node,
      WorkflowInstance instance,
      NodeInstance nodeInstance,
      ExecutionContext context,
      List<WorkflowEdge> outgoingEdges) {
    return NodeExecutionResult.completed(Map.of());
  }
}
