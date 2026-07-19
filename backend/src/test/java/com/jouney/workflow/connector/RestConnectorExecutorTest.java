package com.jouney.workflow.connector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jouney.workflow.definition.WorkflowGraph.WorkflowNode;
import com.jouney.workflow.runtime.ExecutionContext;
import com.jouney.workflow.runtime.NodeExecutionResult;
import com.jouney.workflow.runtime.NodeInstance;
import com.jouney.workflow.runtime.RestServiceTaskNodeExecutor;
import com.jouney.workflow.runtime.WorkflowInstance;
import com.jouney.workflow.shared.WorkflowMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** T051 — mapeamento de entrada/saída e política de retry (seção 12). */
class RestConnectorExecutorTest {

  private final ConnectorDefinitionRepository connectorRepository =
      mock(ConnectorDefinitionRepository.class);
  private final WorkflowMetrics metrics = new WorkflowMetrics(new SimpleMeterRegistry());

  @Test
  void retriesOnTemporaryFailureUntilSuccess() {
    AtomicInteger calls = new AtomicInteger();
    ConnectorExecutor flaky =
        fakeExecutor(
            (connector, operation, input, timeout, key) -> {
              if (calls.incrementAndGet() < 3) {
                return ConnectorResult.failure("temporary", true);
              }
              return ConnectorResult.success(Map.of("result", "APPROVED"));
            });

    RestServiceTaskNodeExecutor executor =
        new RestServiceTaskNodeExecutor(
            connectorRepository, List.of(flaky), Duration.ofSeconds(5), 3, metrics);
    when(connectorRepository.findById("customer-api"))
        .thenReturn(Optional.of(new ConnectorDefinition("customer-api", "REST", "{}", "ref")));

    WorkflowNode node =
        new WorkflowNode(
            "svc-1",
            "REST_SERVICE_TASK",
            "Validar",
            Map.of(
                "connectorId",
                "customer-api",
                "operation",
                "POST /validate",
                "outputMapping",
                Map.of("validationResult", "$.result")));

    NodeExecutionResult result =
        executor.execute(
            node, instance(), nodeInstance(), new ExecutionContext(Map.of()), List.of());

    assertThat(result.outcome()).isEqualTo(NodeExecutionResult.Outcome.COMPLETED);
    assertThat(calls.get()).isEqualTo(3);
    assertThat(result.outputVariables()).containsEntry("validationResult", "APPROVED");
  }

  @Test
  void stopsRetryingAfterMaxAttemptsAndFails() {
    ConnectorExecutor alwaysFails =
        fakeExecutor((c, o, i, t, k) -> ConnectorResult.failure("down", true));
    RestServiceTaskNodeExecutor executor =
        new RestServiceTaskNodeExecutor(
            connectorRepository, List.of(alwaysFails), Duration.ofSeconds(5), 2, metrics);
    when(connectorRepository.findById("customer-api"))
        .thenReturn(Optional.of(new ConnectorDefinition("customer-api", "REST", "{}", "ref")));

    WorkflowNode node =
        new WorkflowNode(
            "svc-1",
            "REST_SERVICE_TASK",
            "Validar",
            Map.of("connectorId", "customer-api", "operation", "POST /validate"));

    NodeExecutionResult result =
        executor.execute(
            node, instance(), nodeInstance(), new ExecutionContext(Map.of()), List.of());

    assertThat(result.outcome()).isEqualTo(NodeExecutionResult.Outcome.FAILED);
    assertThat(result.errorCategory()).isEqualTo(NodeExecutionResult.ErrorCategory.TEMPORARY);
  }

  @Test
  void missingConnectorFailsWithConfigurationErrorWithoutRetrying() {
    RestServiceTaskNodeExecutor executor =
        new RestServiceTaskNodeExecutor(
            connectorRepository, List.of(), Duration.ofSeconds(5), 3, metrics);

    WorkflowNode node =
        new WorkflowNode(
            "svc-1",
            "REST_SERVICE_TASK",
            "Validar",
            Map.of("connectorId", "unknown", "operation", "POST /x"));

    NodeExecutionResult result =
        executor.execute(
            node, instance(), nodeInstance(), new ExecutionContext(Map.of()), List.of());

    assertThat(result.outcome()).isEqualTo(NodeExecutionResult.Outcome.FAILED);
    assertThat(result.errorCategory()).isEqualTo(NodeExecutionResult.ErrorCategory.CONFIGURATION);
  }

  private WorkflowInstance instance() {
    return new WorkflowInstance(UUID.randomUUID(), "biz", "alice");
  }

  private NodeInstance nodeInstance() {
    return new NodeInstance(UUID.randomUUID(), "svc-1", "REST_SERVICE_TASK", 1);
  }

  private ConnectorExecutor fakeExecutor(Fn fn) {
    return new ConnectorExecutor() {
      @Override
      public boolean supports(String connectorType) {
        return "REST".equals(connectorType);
      }

      @Override
      public ConnectorResult execute(
          ConnectorDefinition connector,
          String operation,
          Map<String, Object> input,
          Duration timeout,
          String idempotencyKey) {
        return fn.apply(connector, operation, input, timeout, idempotencyKey);
      }
    };
  }

  private interface Fn {
    ConnectorResult apply(
        ConnectorDefinition c,
        String operation,
        Map<String, Object> input,
        Duration timeout,
        String key);
  }
}
