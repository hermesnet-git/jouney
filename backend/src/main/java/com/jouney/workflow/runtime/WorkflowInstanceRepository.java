package com.jouney.workflow.runtime;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, UUID> {
  List<WorkflowInstance> findAllByOrderByStartedAtDesc();
}
