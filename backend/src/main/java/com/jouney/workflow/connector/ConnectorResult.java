package com.jouney.workflow.connector;

import java.util.Map;

public record ConnectorResult(
    boolean success, Map<String, Object> output, String errorMessage, boolean temporary) {

  public static ConnectorResult success(Map<String, Object> output) {
    return new ConnectorResult(true, output, null, false);
  }

  public static ConnectorResult failure(String errorMessage, boolean temporary) {
    return new ConnectorResult(false, Map.of(), errorMessage, temporary);
  }
}
