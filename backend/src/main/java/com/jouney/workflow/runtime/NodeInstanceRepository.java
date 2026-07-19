package com.jouney.workflow.runtime;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NodeInstanceRepository extends JpaRepository<NodeInstance, UUID> {

  List<NodeInstance> findByWorkflowInstanceIdOrderByStartedAtAsc(UUID workflowInstanceId);

  Optional<NodeInstance> findFirstByWorkflowInstanceIdAndNodeIdOrderByAttemptDesc(
      UUID workflowInstanceId, String nodeId);
}
