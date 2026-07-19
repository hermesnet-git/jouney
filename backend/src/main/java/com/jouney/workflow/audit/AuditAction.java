package com.jouney.workflow.audit;

/** FR-021: ações auditáveis (publicação, execução, tarefa, operação). */
public enum AuditAction {
  WORKFLOW_PUBLISHED,
  EXECUTION_STARTED,
  EXECUTION_COMPLETED,
  EXECUTION_CANCELLED,
  EXECUTION_RETRIED,
  TASK_COMPLETED
}
