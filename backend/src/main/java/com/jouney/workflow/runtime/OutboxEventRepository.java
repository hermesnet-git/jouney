package com.jouney.workflow.runtime;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
  List<OutboxEvent> findByStatus(String status);
}
