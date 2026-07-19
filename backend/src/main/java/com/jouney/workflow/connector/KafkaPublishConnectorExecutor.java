package com.jouney.workflow.connector;

import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * T064 — publica em um tópico Kafka. Nenhum cliente Kafka real está conectado neste MVP (nenhum
 * broker disponível no ambiente de desenvolvimento); a publicação é registrada via log estruturado,
 * mantendo a mesma interface {@link ConnectorExecutor} usada por REST, então trocar isto por um
 * `KafkaTemplate` real depois não muda o motor nem o Outbox Publisher.
 *
 * <p>ponytail: publicação simulada via log, trocar por org.springframework.kafka.core.KafkaTemplate
 * quando houver um broker Kafka configurado no ambiente de destino.
 */
@Component
public class KafkaPublishConnectorExecutor implements ConnectorExecutor {

  private static final Logger log = LoggerFactory.getLogger(KafkaPublishConnectorExecutor.class);

  @Override
  public boolean supports(String connectorType) {
    return "KAFKA".equals(connectorType);
  }

  @Override
  public ConnectorResult execute(
      ConnectorDefinition connector,
      String topic,
      Map<String, Object> input,
      Duration timeout,
      String idempotencyKey) {
    log.info(
        "Publicando evento Kafka: connector={} topic={} idempotencyKey={} payload={}",
        connector.getId(),
        topic,
        idempotencyKey,
        input);
    return ConnectorResult.success(Map.of("topic", topic));
  }
}
