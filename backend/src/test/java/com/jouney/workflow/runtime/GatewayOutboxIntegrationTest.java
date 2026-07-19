package com.jouney.workflow.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jouney.workflow.definition.WorkflowGraph.WorkflowEdge;
import com.jouney.workflow.definition.WorkflowGraph.WorkflowNode;
import com.jouney.workflow.expression.ExpressionEvaluator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * T060 — caminho de decisão publica evento via outbox (FR-018). Testado no nível do
 * ExclusiveGatewayNodeExecutor com um OutboxEventRepository mockado: verifica a gravação do evento
 * sem depender de Docker/Postgres — a consistência transacional em si vem de graça por ambos serem
 * chamados dentro do mesmo método @Transactional do EngineDispatcher (T036/T065).
 */
class GatewayOutboxIntegrationTest {

  private final OutboxEventRepository outboxEventRepository = mock(OutboxEventRepository.class);
  private final ExclusiveGatewayNodeExecutor executor =
      new ExclusiveGatewayNodeExecutor(
          new ExpressionEvaluator(), outboxEventRepository, new ObjectMapper());

  private final WorkflowNode gatewayNode =
      new WorkflowNode("gw-1", "EXCLUSIVE_GATEWAY", "Aprovado?", Map.of());

  private final List<WorkflowEdge> edges =
      List.of(
          new WorkflowEdge(
              "e2",
              "gw-1",
              "end-approved",
              "status == 'APPROVED'",
              Map.of("connectorId", "events", "topic", "customer.approved", "payload", Map.of())),
          new WorkflowEdge("e3", "gw-1", "end-rejected", "status == 'REJECTED'", null));

  @Test
  void approvedPathPublishesOutboxEventAndFollowsCorrectEdge() {
    ExecutionContext context = new ExecutionContext(Map.of("status", "APPROVED"));

    NodeExecutionResult result =
        executor.execute(gatewayNode, instance(), nodeInstance(), context, edges);

    assertThat(result.outcome()).isEqualTo(NodeExecutionResult.Outcome.COMPLETED);
    assertThat(result.nextNodeId()).isEqualTo("end-approved");
    verify(outboxEventRepository, times(1)).save(any(OutboxEvent.class));
  }

  @Test
  void rejectedPathDoesNotPublishAnyEvent() {
    ExecutionContext context = new ExecutionContext(Map.of("status", "REJECTED"));

    NodeExecutionResult result =
        executor.execute(gatewayNode, instance(), nodeInstance(), context, edges);

    assertThat(result.nextNodeId()).isEqualTo("end-rejected");
    verify(outboxEventRepository, times(0)).save(any(OutboxEvent.class));
  }

  @Test
  void unmatchedDataFailsExplicitlyInsteadOfPickingAnArbitraryPath() {
    ExecutionContext context = new ExecutionContext(Map.of("status", "UNKNOWN"));

    NodeExecutionResult result =
        executor.execute(gatewayNode, instance(), nodeInstance(), context, edges);

    assertThat(result.outcome()).isEqualTo(NodeExecutionResult.Outcome.FAILED);
  }

  private WorkflowInstance instance() {
    return new WorkflowInstance(UUID.randomUUID(), "biz", "alice");
  }

  private NodeInstance nodeInstance() {
    return new NodeInstance(UUID.randomUUID(), "gw-1", "EXCLUSIVE_GATEWAY", 1);
  }
}
