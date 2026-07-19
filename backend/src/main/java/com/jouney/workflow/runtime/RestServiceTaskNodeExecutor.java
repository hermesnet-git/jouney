package com.jouney.workflow.runtime;

import com.jouney.workflow.connector.ConnectorDefinition;
import com.jouney.workflow.connector.ConnectorDefinitionRepository;
import com.jouney.workflow.connector.ConnectorExecutor;
import com.jouney.workflow.connector.ConnectorResult;
import com.jouney.workflow.definition.WorkflowGraph.WorkflowEdge;
import com.jouney.workflow.definition.WorkflowGraph.WorkflowNode;
import com.jouney.workflow.shared.NodeType;
import com.jouney.workflow.shared.WorkflowMetrics;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * T055/T056 — chamada a serviço externo com timeout, retry automático (FR-016) e chave de
 * idempotência {@code workflowInstanceId/nodeId/attempt} (seção 12). As tentativas automáticas
 * acontecem aqui, dentro de uma única passada do dispatcher — FR-016 pede retomada automática, não
 * uma nova invocação manual por tentativa.
 */
@Component
public class RestServiceTaskNodeExecutor implements NodeExecutor {

  private final ConnectorDefinitionRepository connectorRepository;
  private final List<ConnectorExecutor> connectorExecutors;
  private final Duration defaultTimeout;
  private final int defaultMaxAttempts;
  private final WorkflowMetrics metrics;

  public RestServiceTaskNodeExecutor(
      ConnectorDefinitionRepository connectorRepository,
      List<ConnectorExecutor> connectorExecutors,
      @Value("${workflow-platform.connector.default-timeout:PT5S}") Duration defaultTimeout,
      @Value("${workflow-platform.connector.default-max-attempts:3}") int defaultMaxAttempts,
      WorkflowMetrics metrics) {
    this.connectorRepository = connectorRepository;
    this.connectorExecutors = connectorExecutors;
    this.defaultTimeout = defaultTimeout;
    this.defaultMaxAttempts = defaultMaxAttempts;
    this.metrics = metrics;
  }

  @Override
  public boolean supports(String nodeType) {
    return NodeType.REST_SERVICE_TASK.name().equals(nodeType);
  }

  @Override
  @SuppressWarnings("unchecked")
  public NodeExecutionResult execute(
      WorkflowNode node,
      WorkflowInstance instance,
      NodeInstance nodeInstance,
      ExecutionContext context,
      List<WorkflowEdge> outgoingEdges) {
    Map<String, Object> config = node.configuration() == null ? Map.of() : node.configuration();
    String connectorId = String.valueOf(config.get("connectorId"));
    String operation = String.valueOf(config.get("operation"));

    ConnectorDefinition connector = connectorRepository.findById(connectorId).orElse(null);
    if (connector == null) {
      return NodeExecutionResult.failed(
          "Conector '" + connectorId + "' não está cadastrado.",
          NodeExecutionResult.ErrorCategory.CONFIGURATION);
    }

    ConnectorExecutor executor =
        connectorExecutors.stream()
            .filter(e -> e.supports(connector.getType()))
            .findFirst()
            .orElse(null);
    if (executor == null) {
      return NodeExecutionResult.failed(
          "Nenhum executor disponível para o tipo de conector " + connector.getType(),
          NodeExecutionResult.ErrorCategory.CONFIGURATION);
    }

    Map<String, String> inputMapping =
        (Map<String, String>) config.getOrDefault("inputMapping", Map.of());
    Map<String, String> outputMapping =
        (Map<String, String>) config.getOrDefault("outputMapping", Map.of());
    Map<String, Object> input = resolveInput(inputMapping, context);

    Duration timeout =
        config.get("timeoutSeconds") != null
            ? Duration.ofSeconds(((Number) config.get("timeoutSeconds")).longValue())
            : defaultTimeout;
    int maxAttempts =
        config.get("maxAttempts") != null
            ? ((Number) config.get("maxAttempts")).intValue()
            : defaultMaxAttempts;
    long initialDelayMillis = 200;

    ConnectorResult lastResult = null;
    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
      String idempotencyKey = instance.getId() + "/" + node.id() + "/" + attempt;
      lastResult = executor.execute(connector, operation, input, timeout, idempotencyKey);

      if (lastResult.success()) {
        return NodeExecutionResult.completed(resolveOutput(outputMapping, lastResult.output()));
      }
      if (!lastResult.temporary() || attempt == maxAttempts) {
        break;
      }
      metrics.connectorRetry(connectorId);
      sleep(initialDelayMillis * (1L << (attempt - 1)));
    }

    metrics.connectorError(connectorId);
    NodeExecutionResult.ErrorCategory category =
        lastResult.temporary()
            ? NodeExecutionResult.ErrorCategory.TEMPORARY
            : NodeExecutionResult.ErrorCategory.BUSINESS;
    return NodeExecutionResult.failed(lastResult.errorMessage(), category);
  }

  private Map<String, Object> resolveInput(
      Map<String, String> inputMapping, ExecutionContext context) {
    Map<String, Object> resolved = new HashMap<>();
    inputMapping.forEach(
        (key, expression) -> resolved.put(key, resolveExpression(expression, context)));
    return resolved;
  }

  private Map<String, Object> resolveOutput(
      Map<String, String> outputMapping, Map<String, Object> rawOutput) {
    if (outputMapping.isEmpty()) {
      return Map.of("response", rawOutput);
    }
    Map<String, Object> resolved = new HashMap<>();
    outputMapping.forEach(
        (variableName, path) -> {
          String field = path.replaceAll(".*\\.", "");
          resolved.put(variableName, rawOutput.getOrDefault(field, rawOutput));
        });
    return resolved;
  }

  /**
   * Simplificação do MVP: extrai o nome da variável de "${a.b.c}" e busca `c` direto no contexto.
   */
  private Object resolveExpression(String expression, ExecutionContext context) {
    String variableName = expression.replaceAll("[${}]", "").replaceAll(".*\\.", "");
    return context.get(variableName);
  }

  private void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
