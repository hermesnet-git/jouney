package com.jouney.workflow.publication;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowVersionRepository extends JpaRepository<WorkflowVersion, UUID> {

  List<WorkflowVersion> findByWorkflowDefinitionIdOrderByVersionNumberDesc(
      UUID workflowDefinitionId);

  Optional<WorkflowVersion> findByWorkflowDefinitionIdAndActiveTrue(UUID workflowDefinitionId);

  int countByWorkflowDefinitionId(UUID workflowDefinitionId);
}
