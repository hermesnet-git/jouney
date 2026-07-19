package com.jouney.workflow.definition;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "workflow_definition")
public class WorkflowDefinition {

  @Id private UUID id;

  private String workflowKey;

  private String name;

  private String description;

  /** DRAFT | PUBLISHED (spec FR-005/FR-006). */
  private String status;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private String graphJson;

  private String createdBy;

  private Instant createdAt;

  private Instant updatedAt;

  protected WorkflowDefinition() {}

  public WorkflowDefinition(String workflowKey, String name, String description, String createdBy) {
    this.id = UUID.randomUUID();
    this.workflowKey = workflowKey;
    this.name = name;
    this.description = description;
    this.status = "DRAFT";
    this.graphJson = "{}";
    this.createdBy = createdBy;
    this.createdAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  public void updateDraft(String name, String description, String graphJson) {
    this.name = name;
    this.description = description;
    this.graphJson = graphJson;
    this.updatedAt = Instant.now();
  }

  public void markPublished() {
    this.status = "PUBLISHED";
    this.updatedAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public String getWorkflowKey() {
    return workflowKey;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public String getStatus() {
    return status;
  }

  public String getGraphJson() {
    return graphJson;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
