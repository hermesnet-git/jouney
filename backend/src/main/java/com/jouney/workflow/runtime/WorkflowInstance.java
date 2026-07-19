package com.jouney.workflow.runtime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

/**
 * FR-007/FR-008/FR-020 — execução concreta de uma Workflow Version. Estados e transições: seção 6
 * do planejamento.md / data-model.md. T039: optimistic locking via {@code lockVersion}.
 */
@Entity
@Table(name = "workflow_instance")
public class WorkflowInstance {

  @Id private UUID id;

  private UUID workflowVersionId;

  private String businessKey;

  /** CREATED, RUNNING, WAITING, COMPLETED, FAILED, CANCELLED. */
  private String status;

  private String startedBy;

  private Instant startedAt;

  private Instant completedAt;

  private String currentNodeId;

  @Version private Long lockVersion;

  protected WorkflowInstance() {}

  public WorkflowInstance(UUID workflowVersionId, String businessKey, String startedBy) {
    this.id = UUID.randomUUID();
    this.workflowVersionId = workflowVersionId;
    this.businessKey = businessKey;
    this.startedBy = startedBy;
    this.status = "CREATED";
    this.startedAt = Instant.now();
  }

  public void start() {
    this.status = "RUNNING";
  }

  public void moveTo(String nodeId) {
    this.currentNodeId = nodeId;
  }

  public void waitFor(String nodeId) {
    this.currentNodeId = nodeId;
    this.status = "WAITING";
  }

  public void resume() {
    if (!"WAITING".equals(status)) {
      throw new IllegalStateException(
          "Instância " + id + " não está aguardando (status=" + status + ").");
    }
    this.status = "RUNNING";
  }

  public void complete() {
    this.status = "COMPLETED";
    this.completedAt = Instant.now();
  }

  public void fail() {
    this.status = "FAILED";
    this.completedAt = Instant.now();
  }

  public void cancel() {
    if ("COMPLETED".equals(status) || "CANCELLED".equals(status)) {
      throw new IllegalStateException(
          "Instância " + id + " já está finalizada (status=" + status + ").");
    }
    this.status = "CANCELLED";
    this.completedAt = Instant.now();
  }

  public void retryFromFailed() {
    if (!"FAILED".equals(status)) {
      throw new IllegalStateException(
          "Instância " + id + " não está com falha (status=" + status + ").");
    }
    this.status = "RUNNING";
    this.completedAt = null;
  }

  public UUID getId() {
    return id;
  }

  public UUID getWorkflowVersionId() {
    return workflowVersionId;
  }

  public String getBusinessKey() {
    return businessKey;
  }

  public String getStatus() {
    return status;
  }

  public String getStartedBy() {
    return startedBy;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public String getCurrentNodeId() {
    return currentNodeId;
  }

  public Long getLockVersion() {
    return lockVersion;
  }
}
