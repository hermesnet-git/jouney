package com.jouney.workflow.audit;

import java.time.Instant;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** T072 — trilha de auditoria consultável, filtrável por entidade/ator/período (FR-021, US6). */
@RestController
@RequestMapping("/api/audit")
public class AuditController {

  private final AuditService service;

  public AuditController(AuditService service) {
    this.service = service;
  }

  public record AuditEntryResponse(
      String actor,
      String action,
      String entityType,
      String entityId,
      Instant occurredAt,
      String detailJson) {
    static AuditEntryResponse from(AuditEntry e) {
      return new AuditEntryResponse(
          e.getActor(),
          e.getAction(),
          e.getEntityType(),
          e.getEntityId(),
          e.getOccurredAt(),
          e.getDetailJson());
    }
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('WORKFLOW_OPERATOR', 'PLATFORM_ADMIN')")
  public List<AuditEntryResponse> search(
      @RequestParam(required = false) String actor,
      @RequestParam(required = false) String entityType,
      @RequestParam(required = false) String entityId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant to) {
    return service.search(actor, entityType, entityId, from, to).stream()
        .map(AuditEntryResponse::from)
        .toList();
  }
}
