package com.jouney.workflow.shared;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.springframework.stereotype.Component;

/**
 * T076 — seção 14: instâncias iniciadas/concluídas/com erro, tempos médios por nó, erros por
 * conector, retentativas. Nomes de métrica expostos em /actuator/metrics e /actuator/prometheus.
 */
@Component
public class WorkflowMetrics {

  private final MeterRegistry registry;

  public WorkflowMetrics(MeterRegistry registry) {
    this.registry = registry;
  }

  public void instanceStarted() {
    registry.counter("workflow.instances.started").increment();
  }

  public void instanceCompleted() {
    registry.counter("workflow.instances.completed").increment();
  }

  public void instanceFailed() {
    registry.counter("workflow.instances.failed").increment();
  }

  public void nodeExecutionTime(String nodeType, Duration duration) {
    Timer.builder("workflow.node.execution.time")
        .tag("nodeType", nodeType)
        .register(registry)
        .record(duration);
  }

  public void connectorError(String connectorId) {
    registry.counter("workflow.connector.errors", "connectorId", connectorId).increment();
  }

  public void connectorRetry(String connectorId) {
    registry.counter("workflow.connector.retries", "connectorId", connectorId).increment();
  }

  public void pendingTask() {
    registry.counter("workflow.tasks.created").increment();
  }
}
