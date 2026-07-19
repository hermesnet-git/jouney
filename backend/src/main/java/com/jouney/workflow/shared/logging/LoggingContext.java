package com.jouney.workflow.shared.logging;

import org.slf4j.MDC;

/**
 * Seção 14 do planejamento.md: toda execução carrega correlationId, workflowInstanceId,
 * nodeInstanceId e businessKey — propagados via MDC para aparecerem no log estruturado JSON (ver
 * application.yml logging.pattern.console).
 */
public final class LoggingContext {

  public static final String CORRELATION_ID = "correlationId";
  public static final String WORKFLOW_INSTANCE_ID = "workflowInstanceId";
  public static final String NODE_INSTANCE_ID = "nodeInstanceId";
  public static final String BUSINESS_KEY = "businessKey";

  private LoggingContext() {}

  public static void putWorkflowInstanceId(String value) {
    MDC.put(WORKFLOW_INSTANCE_ID, value);
  }

  public static void putNodeInstanceId(String value) {
    MDC.put(NODE_INSTANCE_ID, value);
  }

  public static void putBusinessKey(String value) {
    if (value != null) {
      MDC.put(BUSINESS_KEY, value);
    }
  }

  public static void clear() {
    MDC.remove(WORKFLOW_INSTANCE_ID);
    MDC.remove(NODE_INSTANCE_ID);
    MDC.remove(BUSINESS_KEY);
  }
}
