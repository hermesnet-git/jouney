package com.jouney.workflow.runtime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "workflow_variable")
public class WorkflowVariable {

  @Id private UUID workflowInstanceId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private String variablesJson;

  private Instant updatedAt;

  protected WorkflowVariable() {}

  public WorkflowVariable(UUID workflowInstanceId, String variablesJson) {
    this.workflowInstanceId = workflowInstanceId;
    this.variablesJson = variablesJson;
    this.updatedAt = Instant.now();
  }

  public void update(String variablesJson) {
    this.variablesJson = variablesJson;
    this.updatedAt = Instant.now();
  }

  public UUID getWorkflowInstanceId() {
    return workflowInstanceId;
  }

  public String getVariablesJson() {
    return variablesJson;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
