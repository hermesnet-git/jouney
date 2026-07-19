package com.jouney.workflow.connector;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** FR-014 — integração configurada; a credencial em si fica fora do JSON (seção 10/13). */
@Entity
@Table(name = "connector_definition")
public class ConnectorDefinition {

  @Id private String id;

  /** REST no MVP; KAFKA/RABBITMQ preparados para US5. */
  private String type;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private String baseConfigJson;

  private String credentialRef;

  private Instant createdAt;

  private Instant updatedAt;

  protected ConnectorDefinition() {}

  public ConnectorDefinition(String id, String type, String baseConfigJson, String credentialRef) {
    this.id = id;
    this.type = type;
    this.baseConfigJson = baseConfigJson;
    this.credentialRef = credentialRef;
    this.createdAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  public void update(String baseConfigJson, String credentialRef) {
    this.baseConfigJson = baseConfigJson;
    this.credentialRef = credentialRef;
    this.updatedAt = Instant.now();
  }

  public String getId() {
    return id;
  }

  public String getType() {
    return type;
  }

  public String getBaseConfigJson() {
    return baseConfigJson;
  }

  public String getCredentialRef() {
    return credentialRef;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
