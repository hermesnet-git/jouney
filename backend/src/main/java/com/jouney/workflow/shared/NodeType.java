package com.jouney.workflow.shared;

/** Os 5 tipos de etapa do MVP (spec FR-002). */
public enum NodeType {
  START,
  END,
  USER_TASK,
  REST_SERVICE_TASK,
  EXCLUSIVE_GATEWAY
}
