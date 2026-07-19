package com.jouney.workflow.runtime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * T063/FR-018 — Outbox Pattern (seção 12): o evento é gravado na mesma transação que atualiza a
 * instância; um publicador assíncrono o envia depois, garantindo consistência.
 */
@Entity
@Table(name = "outbox_event")
public class OutboxEvent {

  @Id private UUID id;

  private String aggregateType;

  private UUID aggregateId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private String payloadJson;

  /** PENDING, PUBLISHED, FAILED. */
  private String status;

  private Instant createdAt;

  private Instant publishedAt;

  protected OutboxEvent() {}

  public OutboxEvent(String aggregateType, UUID aggregateId, String payloadJson) {
    this.id = UUID.randomUUID();
    this.aggregateType = aggregateType;
    this.aggregateId = aggregateId;
    this.payloadJson = payloadJson;
    this.status = "PENDING";
    this.createdAt = Instant.now();
  }

  public void markPublished() {
    this.status = "PUBLISHED";
    this.publishedAt = Instant.now();
  }

  public void markFailed() {
    this.status = "FAILED";
  }

  public UUID getId() {
    return id;
  }

  public String getAggregateType() {
    return aggregateType;
  }

  public UUID getAggregateId() {
    return aggregateId;
  }

  public String getPayloadJson() {
    return payloadJson;
  }

  public String getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getPublishedAt() {
    return publishedAt;
  }
}
