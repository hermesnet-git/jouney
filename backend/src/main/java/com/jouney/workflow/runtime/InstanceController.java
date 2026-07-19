package com.jouney.workflow.runtime;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * T037/T038/T069/T070 — iniciar, consultar, cancelar e reprocessar execuções (FR-007/009/019/020).
 */
@RestController
@RequestMapping("/api")
public class InstanceController {

  private final InstanceService service;

  public InstanceController(InstanceService service) {
    this.service = service;
  }

  public record StartRequest(String businessKey) {}

  public record InstanceResponse(
      UUID id,
      UUID workflowVersionId,
      String businessKey,
      String status,
      String currentNodeId,
      Instant startedAt,
      Instant completedAt) {
    static InstanceResponse from(WorkflowInstance i) {
      return new InstanceResponse(
          i.getId(),
          i.getWorkflowVersionId(),
          i.getBusinessKey(),
          i.getStatus(),
          i.getCurrentNodeId(),
          i.getStartedAt(),
          i.getCompletedAt());
    }
  }

  public record NodeInstanceResponse(
      UUID id,
      String nodeId,
      String nodeType,
      String status,
      int attempt,
      Instant startedAt,
      Instant completedAt) {
    static NodeInstanceResponse from(NodeInstance n) {
      return new NodeInstanceResponse(
          n.getId(),
          n.getNodeId(),
          n.getNodeType(),
          n.getStatus(),
          n.getAttempt(),
          n.getStartedAt(),
          n.getCompletedAt());
    }
  }

  @PostMapping("/workflow-versions/{versionId}/instances")
  @PreAuthorize("hasRole('WORKFLOW_OPERATOR')")
  public ResponseEntity<InstanceResponse> start(
      @PathVariable UUID versionId,
      @RequestBody(required = false) StartRequest request,
      Authentication auth) {
    String businessKey = request != null ? request.businessKey() : null;
    WorkflowInstance instance = service.start(versionId, businessKey, auth.getName());
    return ResponseEntity.status(201).body(InstanceResponse.from(instance));
  }

  @GetMapping("/instances")
  @PreAuthorize("hasRole('WORKFLOW_OPERATOR')")
  public List<InstanceResponse> list() {
    return service.list().stream().map(InstanceResponse::from).toList();
  }

  @GetMapping("/instances/{id}")
  @PreAuthorize("hasRole('WORKFLOW_OPERATOR')")
  public InstanceResponse get(@PathVariable UUID id) {
    return InstanceResponse.from(service.get(id));
  }

  @GetMapping("/instances/{id}/history")
  @PreAuthorize("hasRole('WORKFLOW_OPERATOR')")
  public List<NodeInstanceResponse> history(@PathVariable UUID id) {
    return service.history(id).stream().map(NodeInstanceResponse::from).toList();
  }

  @NotNull
  @PostMapping("/instances/{id}/cancel")
  @PreAuthorize("hasRole('WORKFLOW_OPERATOR')")
  public ResponseEntity<Void> cancel(@PathVariable UUID id) {
    service.cancel(id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/instances/{id}/retry")
  @PreAuthorize("hasRole('WORKFLOW_OPERATOR')")
  public ResponseEntity<Void> retry(@PathVariable UUID id) {
    service.retry(id);
    return ResponseEntity.noContent().build();
  }
}
