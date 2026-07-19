package com.jouney.workflow.humantask;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** US3 — unidade de trabalho apresentada a um usuário/grupo, ligada a uma Step Instance. */
@Entity
@Table(name = "human_task")
public class HumanTask {

  @Id private UUID id;

  private UUID workflowInstanceId;

  private UUID nodeInstanceId;

  private String name;

  /** PENDING, CLAIMED, COMPLETED, CANCELLED. */
  private String status;

  private String assignee;

  private String candidateGroup;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private String formSchemaJson;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private String formDataJson;

  private Instant createdAt;

  private Instant claimedAt;

  private Instant completedAt;

  protected HumanTask() {}

  public HumanTask(
      UUID workflowInstanceId,
      UUID nodeInstanceId,
      String name,
      String assignee,
      String candidateGroup,
      String formSchemaJson) {
    this.id = UUID.randomUUID();
    this.workflowInstanceId = workflowInstanceId;
    this.nodeInstanceId = nodeInstanceId;
    this.name = name;
    this.status = "PENDING";
    this.assignee = assignee;
    this.candidateGroup = candidateGroup;
    this.formSchemaJson = formSchemaJson;
    this.createdAt = Instant.now();
  }

  /** T045 — assumir é exclusivo: só passa de PENDING para CLAIMED uma vez. */
  public void claim(String user) {
    if (!"PENDING".equals(status)) {
      throw new IllegalStateException("Tarefa " + id + " já foi assumida ou concluída.");
    }
    this.status = "CLAIMED";
    this.assignee = user;
    this.claimedAt = Instant.now();
  }

  public void complete(String formDataJson) {
    this.status = "COMPLETED";
    this.formDataJson = formDataJson;
    this.completedAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public UUID getWorkflowInstanceId() {
    return workflowInstanceId;
  }

  public UUID getNodeInstanceId() {
    return nodeInstanceId;
  }

  public String getName() {
    return name;
  }

  public String getStatus() {
    return status;
  }

  public String getAssignee() {
    return assignee;
  }

  public String getCandidateGroup() {
    return candidateGroup;
  }

  public String getFormSchemaJson() {
    return formSchemaJson;
  }

  public String getFormDataJson() {
    return formDataJson;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getClaimedAt() {
    return claimedAt;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }
}
