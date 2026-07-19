package com.jouney.workflow.humantask;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

/** T048 — inbox de tarefas: listar, assumir e concluir (FR-010, FR-011, FR-012, FR-013). */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

  private final HumanTaskService service;

  public TaskController(HumanTaskService service) {
    this.service = service;
  }

  public record TaskResponse(
      UUID id,
      String name,
      String status,
      String assignee,
      String candidateGroup,
      String formSchemaJson,
      Instant createdAt) {
    static TaskResponse from(HumanTask t) {
      return new TaskResponse(
          t.getId(),
          t.getName(),
          t.getStatus(),
          t.getAssignee(),
          t.getCandidateGroup(),
          t.getFormSchemaJson(),
          t.getCreatedAt());
    }
  }

  public record CompleteRequest(Map<String, Object> formData) {}

  @GetMapping
  @PreAuthorize("hasRole('TASK_USER')")
  public List<TaskResponse> list(Authentication auth) {
    return service.listPending(auth.getName(), groupsOf(auth)).stream()
        .map(TaskResponse::from)
        .toList();
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('TASK_USER')")
  public TaskResponse get(@PathVariable UUID id) {
    return TaskResponse.from(service.get(id));
  }

  @PostMapping("/{id}/claim")
  @PreAuthorize("hasRole('TASK_USER')")
  public TaskResponse claim(@PathVariable UUID id, Authentication auth) {
    return TaskResponse.from(service.claim(id, auth.getName()));
  }

  @PostMapping("/{id}/complete")
  @PreAuthorize("hasRole('TASK_USER')")
  public void complete(@PathVariable UUID id, @RequestBody CompleteRequest request) {
    service.complete(id, request.formData());
  }

  @SuppressWarnings("unchecked")
  private List<String> groupsOf(Authentication auth) {
    if (auth instanceof JwtAuthenticationToken jwtAuth) {
      Jwt jwt = jwtAuth.getToken();
      List<String> groups = jwt.getClaimAsStringList("groups");
      return groups != null ? groups : List.of();
    }
    return List.of();
  }
}
