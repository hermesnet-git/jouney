package com.jouney.workflow.audit;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditEntryRepository extends JpaRepository<AuditEntry, UUID> {

  @Query(
      """
            select a from AuditEntry a
            where (:actor is null or a.actor = :actor)
              and (:entityType is null or a.entityType = :entityType)
              and (:entityId is null or a.entityId = :entityId)
              and (:from is null or a.occurredAt >= :from)
              and (:to is null or a.occurredAt <= :to)
            order by a.occurredAt desc
            """)
  List<AuditEntry> search(
      @Param("actor") String actor,
      @Param("entityType") String entityType,
      @Param("entityId") String entityId,
      @Param("from") Instant from,
      @Param("to") Instant to);
}
