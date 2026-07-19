package com.jouney.workflow.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "audit_entry")
public class AuditEntry {

  @Id private UUID id;

  private String actor;

  private String action;

  private String entityType;

  private String entityId;

  private Instant occurredAt;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private String detailJson;

  protected AuditEntry() {}

  public AuditEntry(
      UUID id,
      String actor,
      String action,
      String entityType,
      String entityId,
      Instant occurredAt,
      String detailJson) {
    this.id = id;
    this.actor = actor;
    this.action = action;
    this.entityType = entityType;
    this.entityId = entityId;
    this.occurredAt = occurredAt;
    this.detailJson = detailJson;
  }

  public static AuditEntry of(
      String actor, AuditAction action, String entityType, String entityId, String detailJson) {
    return new AuditEntry(
        UUID.randomUUID(), actor, action.name(), entityType, entityId, Instant.now(), detailJson);
  }

  public UUID getId() {
    return id;
  }

  public String getActor() {
    return actor;
  }

  public String getAction() {
    return action;
  }

  public String getEntityType() {
    return entityType;
  }

  public String getEntityId() {
    return entityId;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public String getDetailJson() {
    return detailJson;
  }
}
