package com.jouney.workflow.connector;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** T057 — cadastro, consulta e teste de conectores (FR-014, seção 10/13). */
@RestController
@RequestMapping("/api/connectors")
public class ConnectorController {

  private final ConnectorService service;

  public ConnectorController(ConnectorService service) {
    this.service = service;
  }

  public record CreateRequest(
      @NotBlank String id,
      @NotBlank String type,
      String baseConfigJson,
      @NotBlank String credentialRef) {}

  public record UpdateRequest(String baseConfigJson, String credentialRef) {}

  public record TestRequest(String operation) {}

  public record Response(
      String id, String type, String baseConfigJson, String credentialRef, Instant updatedAt) {
    static Response from(ConnectorDefinition c) {
      return new Response(
          c.getId(), c.getType(), c.getBaseConfigJson(), c.getCredentialRef(), c.getUpdatedAt());
    }
  }

  public record TestResponse(boolean success, String errorMessage) {}

  @PostMapping
  @PreAuthorize("hasRole('PLATFORM_ADMIN')")
  public ResponseEntity<Response> create(@RequestBody CreateRequest request) {
    ConnectorDefinition created =
        service.create(
            request.id(), request.type(), request.baseConfigJson(), request.credentialRef());
    return ResponseEntity.status(201).body(Response.from(created));
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('WORKFLOW_DESIGNER', 'PLATFORM_ADMIN')")
  public List<Response> list() {
    return service.list().stream().map(Response::from).toList();
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('WORKFLOW_DESIGNER', 'PLATFORM_ADMIN')")
  public Response get(@PathVariable String id) {
    return Response.from(service.get(id));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('PLATFORM_ADMIN')")
  public Response update(@PathVariable String id, @RequestBody UpdateRequest request) {
    return Response.from(service.update(id, request.baseConfigJson(), request.credentialRef()));
  }

  @PostMapping("/{id}/test")
  @PreAuthorize("hasRole('PLATFORM_ADMIN')")
  public TestResponse test(@PathVariable String id, @RequestBody TestRequest request) {
    ConnectorResult result = service.test(id, request.operation());
    return new TestResponse(result.success(), result.errorMessage());
  }
}
