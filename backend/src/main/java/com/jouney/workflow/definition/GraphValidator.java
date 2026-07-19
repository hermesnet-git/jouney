package com.jouney.workflow.definition;

import com.jouney.workflow.definition.WorkflowGraph.WorkflowEdge;
import com.jouney.workflow.definition.WorkflowGraph.WorkflowNode;
import com.jouney.workflow.shared.NodeType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * FR-004: valida um workflow antes da publicação e retorna TODOS os problemas de uma vez (não para
 * no primeiro erro encontrado).
 */
@Component
public class GraphValidator {

  public List<String> validate(WorkflowGraph graph) {
    List<String> problems = new ArrayList<>();
    if (graph == null || graph.nodes() == null || graph.nodes().isEmpty()) {
      return List.of("O workflow não tem nenhuma etapa.");
    }

    Map<String, WorkflowNode> nodesById =
        graph.nodes().stream().collect(Collectors.toMap(WorkflowNode::id, n -> n, (a, b) -> a));
    List<WorkflowEdge> edges = graph.edges() == null ? List.of() : graph.edges();

    List<WorkflowNode> startNodes =
        graph.nodes().stream().filter(n -> NodeType.START.name().equals(n.type())).toList();
    if (startNodes.isEmpty()) {
      problems.add("O workflow não tem uma etapa de início (START).");
    } else if (startNodes.size() > 1) {
      problems.add("O workflow tem mais de uma etapa de início (START).");
    }

    boolean hasEnd = graph.nodes().stream().anyMatch(n -> NodeType.END.name().equals(n.type()));
    if (!hasEnd) {
      problems.add("O workflow não tem uma etapa de fim (END).");
    }

    Set<String> connectedNodeIds = new HashSet<>();
    for (WorkflowEdge edge : edges) {
      connectedNodeIds.add(edge.source());
      connectedNodeIds.add(edge.target());
      if (!nodesById.containsKey(edge.source())) {
        problems.add(
            "Aresta '" + edge.id() + "' referencia um nó de origem inexistente: " + edge.source());
      }
      if (!nodesById.containsKey(edge.target())) {
        problems.add(
            "Aresta '" + edge.id() + "' referencia um nó de destino inexistente: " + edge.target());
      }
    }

    for (WorkflowNode node : graph.nodes()) {
      boolean isStart = NodeType.START.name().equals(node.type());
      boolean isEnd = NodeType.END.name().equals(node.type());
      if (!isStart && !isEnd && !connectedNodeIds.contains(node.id())) {
        problems.add("Etapa '" + node.name() + "' (" + node.id() + ") está desconectada.");
      }
      if (NodeType.USER_TASK.name().equals(node.type())) {
        Object formKey = node.configuration() == null ? null : node.configuration().get("formKey");
        if (formKey == null || formKey.toString().isBlank()) {
          problems.add("Tarefa humana '" + node.name() + "' não tem um formulário configurado.");
        }
      }
      if (NodeType.REST_SERVICE_TASK.name().equals(node.type())) {
        Object connectorId =
            node.configuration() == null ? null : node.configuration().get("connectorId");
        if (connectorId == null || connectorId.toString().isBlank()) {
          problems.add("Chamada externa '" + node.name() + "' não tem um conector configurado.");
        }
      }
      if (NodeType.EXCLUSIVE_GATEWAY.name().equals(node.type())) {
        long outgoing = edges.stream().filter(e -> e.source().equals(node.id())).count();
        long withCondition =
            edges.stream()
                .filter(
                    e ->
                        e.source().equals(node.id())
                            && e.condition() != null
                            && !e.condition().isBlank())
                .count();
        if (outgoing < 2) {
          problems.add("Decisão '" + node.name() + "' precisa de pelo menos 2 caminhos de saída.");
        } else if (withCondition < outgoing) {
          problems.add(
              "Decisão '" + node.name() + "' tem caminho(s) de saída sem condição configurada.");
        }
      }
    }

    problems.addAll(findUnreachableNodes(graph, edges, startNodes));

    return problems;
  }

  private List<String> findUnreachableNodes(
      WorkflowGraph graph, List<WorkflowEdge> edges, List<WorkflowNode> startNodes) {
    if (startNodes.isEmpty()) {
      return List.of();
    }
    Set<String> reachable = new HashSet<>();
    java.util.Deque<String> toVisit = new java.util.ArrayDeque<>();
    toVisit.add(startNodes.get(0).id());
    while (!toVisit.isEmpty()) {
      String current = toVisit.poll();
      if (!reachable.add(current)) {
        continue;
      }
      edges.stream().filter(e -> e.source().equals(current)).forEach(e -> toVisit.add(e.target()));
    }
    return graph.nodes().stream()
        .filter(n -> !reachable.contains(n.id()))
        .map(n -> "Etapa '" + n.name() + "' (" + n.id() + ") é inalcançável a partir do início.")
        .toList();
  }
}
