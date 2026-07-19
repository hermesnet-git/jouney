package com.jouney.workflow.runtime;

import java.util.Map;

/**
 * Resultado da execução de um nó (seção 12 do planejamento.md distingue erro temporário, de
 * negócio, de configuração e não recuperável — ver {@link ErrorCategory}).
 */
public record NodeExecutionResult(
    Outcome outcome,
    Map<String, Object> outputVariables,
    String errorMessage,
    ErrorCategory errorCategory,
    String nextNodeId) {

  public enum Outcome {
    COMPLETED,
    WAITING,
    FAILED
  }

  public enum ErrorCategory {
    TEMPORARY,
    BUSINESS,
    CONFIGURATION,
    NON_RECOVERABLE
  }

  public static NodeExecutionResult completed(Map<String, Object> outputVariables) {
    return new NodeExecutionResult(Outcome.COMPLETED, outputVariables, null, null, null);
  }

  public static NodeExecutionResult completed(
      Map<String, Object> outputVariables, String nextNodeId) {
    return new NodeExecutionResult(Outcome.COMPLETED, outputVariables, null, null, nextNodeId);
  }

  public static NodeExecutionResult waiting() {
    return new NodeExecutionResult(Outcome.WAITING, Map.of(), null, null, null);
  }

  public static NodeExecutionResult failed(String message, ErrorCategory category) {
    return new NodeExecutionResult(Outcome.FAILED, Map.of(), message, category, null);
  }
}
