package com.jouney.workflow.runtime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** FR-009 — execução de uma etapa específica dentro de uma Workflow Instance. */
@Entity
@Table(name = "node_instance")
public class NodeInstance {

  @Id private UUID id;

  private UUID workflowInstanceId;

  private String nodeId;

  private String nodeType;

  /** PENDING, RUNNING, WAITING, COMPLETED, FAILED, CANCELLED, SKIPPED. */
  private String status;

  private int attempt;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private String inputJson;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private String outputJson;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private String errorJson;

  private Instant startedAt;

  private Instant completedAt;

  protected NodeInstance() {}

  public NodeInstance(UUID workflowInstanceId, String nodeId, String nodeType, int attempt) {
    this.id = UUID.randomUUID();
    this.workflowInstanceId = workflowInstanceId;
    this.nodeId = nodeId;
    this.nodeType = nodeType;
    this.attempt = attempt;
    this.status = "RUNNING";
    this.startedAt = Instant.now();
  }

  public void complete(String outputJson) {
    this.status = "COMPLETED";
    this.outputJson = outputJson;
    this.completedAt = Instant.now();
  }

  public void waitState() {
    this.status = "WAITING";
  }

  public void fail(String errorJson) {
    this.status = "FAILED";
    this.errorJson = errorJson;
    this.completedAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public UUID getWorkflowInstanceId() {
    return workflowInstanceId;
  }

  public String getNodeId() {
    return nodeId;
  }

  public String getNodeType() {
    return nodeType;
  }

  public String getStatus() {
    return status;
  }

  public int getAttempt() {
    return attempt;
  }

  public String getInputJson() {
    return inputJson;
  }

  public String getOutputJson() {
    return outputJson;
  }

  public String getErrorJson() {
    return errorJson;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }
}
