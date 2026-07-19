package com.jouney.workflow.publication;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class PublicationController {

  private final PublicationService service;

  public PublicationController(PublicationService service) {
    this.service = service;
  }

  public record VersionResponse(
      UUID id, int versionNumber, String publishedBy, Instant publishedAt, boolean active) {
    static VersionResponse from(WorkflowVersion v) {
      return new VersionResponse(
          v.getId(), v.getVersionNumber(), v.getPublishedBy(), v.getPublishedAt(), v.isActive());
    }
  }

  @PostMapping("/workflows/{id}/publish")
  @PreAuthorize("hasRole('WORKFLOW_PUBLISHER')")
  public ResponseEntity<VersionResponse> publish(
      @PathVariable("id") UUID workflowDefinitionId, Authentication auth) {
    WorkflowVersion version = service.publish(workflowDefinitionId, auth.getName());
    return ResponseEntity.status(201).body(VersionResponse.from(version));
  }

  @GetMapping("/workflows/{id}/versions")
  @PreAuthorize("hasAnyRole('WORKFLOW_DESIGNER', 'WORKFLOW_OPERATOR')")
  public List<VersionResponse> listVersions(@PathVariable("id") UUID workflowDefinitionId) {
    return service.listVersions(workflowDefinitionId).stream().map(VersionResponse::from).toList();
  }
}
