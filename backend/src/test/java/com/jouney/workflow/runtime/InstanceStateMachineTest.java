package com.jouney.workflow.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * T032 — transições de estado da instância (seção 6:
 * CREATED→RUNNING→...→COMPLETED/FAILED/CANCELLED).
 */
class InstanceStateMachineTest {

  @Test
  void newInstanceStartsAsCreated() {
    WorkflowInstance instance = new WorkflowInstance(UUID.randomUUID(), "biz-1", "alice");
    assertThat(instance.getStatus()).isEqualTo("CREATED");
  }

  @Test
  void startMovesToRunning() {
    WorkflowInstance instance = new WorkflowInstance(UUID.randomUUID(), null, "alice");
    instance.start();
    assertThat(instance.getStatus()).isEqualTo("RUNNING");
  }

  @Test
  void waitForMovesToWaitingAtGivenNode() {
    WorkflowInstance instance = new WorkflowInstance(UUID.randomUUID(), null, "alice");
    instance.start();
    instance.waitFor("task-1");
    assertThat(instance.getStatus()).isEqualTo("WAITING");
    assertThat(instance.getCurrentNodeId()).isEqualTo("task-1");
  }

  @Test
  void resumeOnlyAllowedFromWaiting() {
    WorkflowInstance instance = new WorkflowInstance(UUID.randomUUID(), null, "alice");
    instance.start();
    assertThatThrownBy(instance::resume).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void completeSetsCompletedAt() {
    WorkflowInstance instance = new WorkflowInstance(UUID.randomUUID(), null, "alice");
    instance.start();
    instance.complete();
    assertThat(instance.getStatus()).isEqualTo("COMPLETED");
    assertThat(instance.getCompletedAt()).isNotNull();
  }

  @Test
  void cancelNotAllowedWhenAlreadyCompleted() {
    WorkflowInstance instance = new WorkflowInstance(UUID.randomUUID(), null, "alice");
    instance.start();
    instance.complete();
    assertThatThrownBy(instance::cancel).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void retryFromFailedOnlyAllowedWhenFailed() {
    WorkflowInstance instance = new WorkflowInstance(UUID.randomUUID(), null, "alice");
    instance.start();
    assertThatThrownBy(instance::retryFromFailed).isInstanceOf(IllegalStateException.class);

    instance.fail();
    instance.retryFromFailed();
    assertThat(instance.getStatus()).isEqualTo("RUNNING");
  }
}
