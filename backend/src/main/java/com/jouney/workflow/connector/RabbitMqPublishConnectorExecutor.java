package com.jouney.workflow.connector;

import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * T064 — publica em uma fila/exchange RabbitMQ. Mesma observação de {@link
 * KafkaPublishConnectorExecutor}: sem broker disponível neste ambiente, publica via log estruturado
 * atrás da mesma interface {@link ConnectorExecutor}.
 *
 * <p>ponytail: publicação simulada via log, trocar por RabbitTemplate real quando houver um broker
 * RabbitMQ configurado no ambiente de destino.
 */
@Component
public class RabbitMqPublishConnectorExecutor implements ConnectorExecutor {

  private static final Logger log = LoggerFactory.getLogger(RabbitMqPublishConnectorExecutor.class);

  @Override
  public boolean supports(String connectorType) {
    return "RABBITMQ".equals(connectorType);
  }

  @Override
  public ConnectorResult execute(
      ConnectorDefinition connector,
      String routingKey,
      Map<String, Object> input,
      Duration timeout,
      String idempotencyKey) {
    log.info(
        "Publicando evento RabbitMQ: connector={} routingKey={} idempotencyKey={} payload={}",
        connector.getId(),
        routingKey,
        idempotencyKey,
        input);
    return ConnectorResult.success(Map.of("routingKey", routingKey));
  }
}
