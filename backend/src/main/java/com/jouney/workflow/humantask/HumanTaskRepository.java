package com.jouney.workflow.humantask;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HumanTaskRepository extends JpaRepository<HumanTask, UUID> {

  Optional<HumanTask> findByNodeInstanceId(UUID nodeInstanceId);

  @Query(
      """
            select t from HumanTask t
            where t.status = 'PENDING'
              and (t.assignee = :user or t.candidateGroup in :groups)
            """)
  List<HumanTask> findPendingForUser(
      @Param("user") String user, @Param("groups") List<String> groups);
}
