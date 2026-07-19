package com.jouney.workflow.connector;

import java.time.Duration;
import java.util.Map;

/** Seção 10 do planejamento.md — cada tipo de integração implementa isto sem alterar o motor. */
public interface ConnectorExecutor {

  boolean supports(String connectorType);

  ConnectorResult execute(
      ConnectorDefinition connector,
      String operation,
      Map<String, Object> input,
      Duration timeout,
      String idempotencyKey);
}
