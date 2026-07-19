package com.jouney.workflow.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/** T013/T071 — grava toda ação relevante numa trilha auditável e consultável (FR-021). */
@Service
public class AuditService {

  private final AuditEntryRepository repository;
  private final ObjectMapper objectMapper;

  public AuditService(AuditEntryRepository repository, ObjectMapper objectMapper) {
    this.repository = repository;
    this.objectMapper = objectMapper;
  }

  public void record(
      String actor,
      AuditAction action,
      String entityType,
      String entityId,
      Map<String, Object> detail) {
    String detailJson = null;
    if (detail != null && !detail.isEmpty()) {
      try {
        detailJson = objectMapper.writeValueAsString(detail);
      } catch (JsonProcessingException e) {
        detailJson = "{}";
      }
    }
    repository.save(AuditEntry.of(actor, action, entityType, entityId, detailJson));
  }

  public List<AuditEntry> search(
      String actor, String entityType, String entityId, Instant from, Instant to) {
    return repository.search(actor, entityType, entityId, from, to);
  }
}
