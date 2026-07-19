package com.jouney.workflow.definition;

import static org.assertj.core.api.Assertions.assertThat;

import com.jouney.workflow.definition.WorkflowGraph.WorkflowEdge;
import com.jouney.workflow.definition.WorkflowGraph.WorkflowNode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GraphValidatorTest {

  private final GraphValidator validator = new GraphValidator();

  @Test
  void validGraphHasNoProblems() {
    WorkflowGraph graph =
        new WorkflowGraph(
            "simple",
            "Simples",
            "start-1",
            List.of(
                new WorkflowNode("start-1", "START", "Início", Map.of()),
                new WorkflowNode("end-1", "END", "Fim", Map.of())),
            List.of(new WorkflowEdge("edge-1", "start-1", "end-1", null, null)));

    assertThat(validator.validate(graph)).isEmpty();
  }

  @Test
  void missingStartIsReported() {
    WorkflowGraph graph =
        new WorkflowGraph(
            "no-start",
            "Sem início",
            null,
            List.of(new WorkflowNode("end-1", "END", "Fim", Map.of())),
            List.of());

    List<String> problems = validator.validate(graph);

    assertThat(problems).anyMatch(p -> p.contains("início"));
  }

  @Test
  void disconnectedNodeIsReported() {
    WorkflowGraph graph =
        new WorkflowGraph(
            "disconnected",
            "Desconectado",
            "start-1",
            List.of(
                new WorkflowNode("start-1", "START", "Início", Map.of()),
                new WorkflowNode("end-1", "END", "Fim", Map.of()),
                new WorkflowNode("orphan-1", "USER_TASK", "Órfão", Map.of("formKey", "x"))),
            List.of(new WorkflowEdge("edge-1", "start-1", "end-1", null, null)));

    List<String> problems = validator.validate(graph);

    assertThat(problems).anyMatch(p -> p.contains("Órfão") && p.contains("desconectada"));
  }

  @Test
  void gatewayWithoutConditionsOnAllPathsIsReported() {
    WorkflowGraph graph =
        new WorkflowGraph(
            "gw",
            "Gateway",
            "start-1",
            List.of(
                new WorkflowNode("start-1", "START", "Início", Map.of()),
                new WorkflowNode("gw-1", "EXCLUSIVE_GATEWAY", "Decisão", Map.of()),
                new WorkflowNode("end-1", "END", "Fim 1", Map.of()),
                new WorkflowNode("end-2", "END", "Fim 2", Map.of())),
            List.of(
                new WorkflowEdge("e1", "start-1", "gw-1", null, null),
                new WorkflowEdge("e2", "gw-1", "end-1", "x == 1", null),
                new WorkflowEdge("e3", "gw-1", "end-2", null, null)));

    List<String> problems = validator.validate(graph);

    assertThat(problems).anyMatch(p -> p.contains("Decisão") && p.contains("sem condição"));
  }

  @Test
  void userTaskWithoutFormIsReported() {
    WorkflowGraph graph =
        new WorkflowGraph(
            "ut",
            "User task",
            "start-1",
            List.of(
                new WorkflowNode("start-1", "START", "Início", Map.of()),
                new WorkflowNode("task-1", "USER_TASK", "Preencher", Map.of()),
                new WorkflowNode("end-1", "END", "Fim", Map.of())),
            List.of(
                new WorkflowEdge("e1", "start-1", "task-1", null, null),
                new WorkflowEdge("e2", "task-1", "end-1", null, null)));

    List<String> problems = validator.validate(graph);

    assertThat(problems).anyMatch(p -> p.contains("formulário"));
  }
}
