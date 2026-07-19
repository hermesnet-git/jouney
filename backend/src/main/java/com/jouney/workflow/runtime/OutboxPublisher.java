package com.jouney.workflow.runtime;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jouney.workflow.connector.ConnectorDefinition;
import com.jouney.workflow.connector.ConnectorDefinitionRepository;
import com.jouney.workflow.connector.ConnectorExecutor;
import com.jouney.workflow.connector.ConnectorResult;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * T065 — publicador assíncrono do Outbox Pattern: lê eventos PENDING gravados por {@link
 * ExclusiveGatewayNodeExecutor} na mesma transação da instância, e os envia via o {@link
 * ConnectorExecutor} do tipo (KAFKA/RABBITMQ) apropriado.
 */
@Component
public class OutboxPublisher {

  private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

  private final OutboxEventRepository outboxEventRepository;
  private final ConnectorDefinitionRepository connectorRepository;
  private final List<ConnectorExecutor> connectorExecutors;
  private final ObjectMapper objectMapper;

  public OutboxPublisher(
      OutboxEventRepository outboxEventRepository,
      ConnectorDefinitionRepository connectorRepository,
      List<ConnectorExecutor> connectorExecutors,
      ObjectMapper objectMapper) {
    this.outboxEventRepository = outboxEventRepository;
    this.connectorRepository = connectorRepository;
    this.connectorExecutors = connectorExecutors;
    this.objectMapper = objectMapper;
  }

  @Scheduled(fixedDelay = 2000)
  @Transactional
  public void publishPending() {
    for (OutboxEvent event : outboxEventRepository.findByStatus("PENDING")) {
      try {
        publish(event);
        event.markPublished();
      } catch (Exception e) {
        log.warn("Falha ao publicar outbox event {}: {}", event.getId(), e.getMessage());
        event.markFailed();
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void publish(OutboxEvent event) throws Exception {
    Map<String, Object> payload =
        objectMapper.readValue(event.getPayloadJson(), new TypeReference<Map<String, Object>>() {});
    String connectorId = String.valueOf(payload.get("connectorId"));
    String topic = String.valueOf(payload.getOrDefault("topic", "default"));
    Map<String, Object> messageBody =
        (Map<String, Object>) payload.getOrDefault("payload", Map.of());

    ConnectorDefinition connector = connectorRepository.findById(connectorId).orElseThrow();
    ConnectorExecutor executor =
        connectorExecutors.stream()
            .filter(e -> e.supports(connector.getType()))
            .findFirst()
            .orElseThrow();

    ConnectorResult result =
        executor.execute(
            connector, topic, messageBody, Duration.ofSeconds(5), event.getId().toString());
    if (!result.success()) {
      throw new IllegalStateException(result.errorMessage());
    }
  }
}
