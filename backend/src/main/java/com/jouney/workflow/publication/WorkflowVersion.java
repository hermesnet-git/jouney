package com.jouney.workflow.publication;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** FR-006: fotografia imutável de uma Workflow Definition no momento da publicação. */
@Entity
@Table(name = "workflow_version")
public class WorkflowVersion {

  @Id private UUID id;

  private UUID workflowDefinitionId;

  private int versionNumber;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private String definitionJson;

  private String publishedBy;

  private Instant publishedAt;

  private boolean active;

  protected WorkflowVersion() {}

  public WorkflowVersion(
      UUID workflowDefinitionId, int versionNumber, String definitionJson, String publishedBy) {
    this.id = UUID.randomUUID();
    this.workflowDefinitionId = workflowDefinitionId;
    this.versionNumber = versionNumber;
    this.definitionJson = definitionJson;
    this.publishedBy = publishedBy;
    this.publishedAt = Instant.now();
    this.active = true;
  }

  public UUID getId() {
    return id;
  }

  public UUID getWorkflowDefinitionId() {
    return workflowDefinitionId;
  }

  public int getVersionNumber() {
    return versionNumber;
  }

  public String getDefinitionJson() {
    return definitionJson;
  }

  public String getPublishedBy() {
    return publishedBy;
  }

  public Instant getPublishedAt() {
    return publishedAt;
  }

  public boolean isActive() {
    return active;
  }

  public void deactivate() {
    this.active = false;
  }
}
