package com.jouney.workflow.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jouney.workflow.audit.AuditAction;
import com.jouney.workflow.audit.AuditEntryRepository;
import com.jouney.workflow.audit.AuditService;
import com.jouney.workflow.shared.WorkflowMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * T068 — FR-020: cancelamento de uma execução em andamento. Só mocka interfaces (repositórios) —
 * mockar classes concretas (EngineDispatcher/AuditService) não funciona neste ambiente por causa de
 * uma incompatibilidade do ByteBuddy do Mockito com o JDK 24 instalado aqui, então o teste usa
 * instâncias reais de AuditService/EngineDispatcher com seus repositórios mockados.
 */
class CancelIntegrationTest {

  private final WorkflowInstanceRepository instanceRepository =
      mock(WorkflowInstanceRepository.class);
  private final AuditEntryRepository auditEntryRepository = mock(AuditEntryRepository.class);
  private final AuditService auditService =
      new AuditService(auditEntryRepository, new ObjectMapper());
  private final EngineDispatcher dispatcher =
      new EngineDispatcher(
          mock(com.jouney.workflow.publication.WorkflowVersionRepository.class),
          instanceRepository,
          mock(NodeInstanceRepository.class),
          mock(WorkflowVariableRepository.class),
          List.of(),
          auditService,
          new ObjectMapper(),
          new WorkflowMetrics(new SimpleMeterRegistry()));
  private final InstanceService service =
      new InstanceService(
          dispatcher, instanceRepository, mock(NodeInstanceRepository.class), auditService);

  @Test
  void cancelMarksRunningInstanceAsCancelledAndRecordsAudit() {
    WorkflowInstance instance = new WorkflowInstance(UUID.randomUUID(), null, "alice");
    instance.start();
    when(instanceRepository.findById(instance.getId())).thenReturn(Optional.of(instance));

    service.cancel(instance.getId());

    assertThat(instance.getStatus()).isEqualTo("CANCELLED");
    verify(auditEntryRepository)
        .save(
            org.mockito.ArgumentMatchers.argThat(
                entry ->
                    entry.getAction().equals(AuditAction.EXECUTION_CANCELLED.name())
                        && entry.getEntityId().equals(instance.getId().toString())));
  }

  @Test
  void cancelIsRejectedWhenInstanceAlreadyCompleted() {
    WorkflowInstance instance = new WorkflowInstance(UUID.randomUUID(), null, "alice");
    instance.start();
    instance.complete();
    when(instanceRepository.findById(instance.getId())).thenReturn(Optional.of(instance));

    assertThatThrownBy(() -> service.cancel(instance.getId()))
        .isInstanceOf(IllegalStateException.class);
  }
}
