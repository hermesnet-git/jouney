package com.jouney.workflow.definition;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition, UUID> {
  Optional<WorkflowDefinition> findByWorkflowKey(String workflowKey);
}
