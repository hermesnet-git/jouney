package com.jouney.workflow.definition;

import com.jouney.workflow.definition.WorkflowDto.CreateRequest;
import com.jouney.workflow.definition.WorkflowDto.Response;
import com.jouney.workflow.definition.WorkflowDto.UpdateRequest;
import com.jouney.workflow.definition.WorkflowDto.ValidationResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** T023/T024 — CRUD de rascunho e validação (FR-001, FR-003, FR-004, FR-005). */
@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {

  private final WorkflowDefinitionService service;

  public WorkflowController(WorkflowDefinitionService service) {
    this.service = service;
  }

  @PostMapping
  @PreAuthorize("hasRole('WORKFLOW_DESIGNER')")
  public ResponseEntity<Response> create(
      @Valid @RequestBody CreateRequest request, Authentication auth) {
    WorkflowDefinition created =
        service.create(
            request.workflowKey(), request.name(), request.description(), auth.getName());
    return ResponseEntity.status(201).body(Response.from(created));
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('WORKFLOW_DESIGNER', 'WORKFLOW_OPERATOR')")
  public List<Response> list() {
    return service.list().stream().map(Response::from).toList();
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('WORKFLOW_DESIGNER', 'WORKFLOW_OPERATOR')")
  public Response get(@PathVariable UUID id) {
    return Response.from(service.get(id));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('WORKFLOW_DESIGNER')")
  public Response update(@PathVariable UUID id, @Valid @RequestBody UpdateRequest request) {
    WorkflowDefinition updated =
        service.updateDraft(id, request.name(), request.description(), request.graphJson());
    return Response.from(updated);
  }

  @PostMapping("/{id}/validate")
  @PreAuthorize("hasRole('WORKFLOW_DESIGNER')")
  public ValidationResponse validate(@PathVariable UUID id) {
    List<String> problems = service.validate(id);
    return new ValidationResponse(problems.isEmpty(), problems);
  }
}
