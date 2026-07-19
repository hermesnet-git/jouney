package com.jouney.workflow.definition;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.UUID;

public final class WorkflowDto {

  private WorkflowDto() {}

  public record CreateRequest(
      @NotBlank String workflowKey, @NotBlank String name, String description) {}

  public record UpdateRequest(@NotBlank String name, String description, String graphJson) {}

  public record Response(
      UUID id,
      String workflowKey,
      String name,
      String description,
      String status,
      String graphJson,
      Instant createdAt,
      Instant updatedAt) {

    public static Response from(WorkflowDefinition d) {
      return new Response(
          d.getId(),
          d.getWorkflowKey(),
          d.getName(),
          d.getDescription(),
          d.getStatus(),
          d.getGraphJson(),
          d.getCreatedAt(),
          d.getUpdatedAt());
    }
  }

  public record ValidationResponse(boolean valid, java.util.List<String> problems) {}
}
