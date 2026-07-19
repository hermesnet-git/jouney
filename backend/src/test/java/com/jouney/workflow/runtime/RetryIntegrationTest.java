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
import com.jouney.workflow.definition.WorkflowGraph.WorkflowEdge;
import com.jouney.workflow.definition.WorkflowGraph.WorkflowNode;
import com.jouney.workflow.publication.WorkflowVersion;
import com.jouney.workflow.publication.WorkflowVersionRepository;
import com.jouney.workflow.shared.WorkflowMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * T067 — FR-020: retry reprocessa só a etapa que falhou, sem repetir etapas já concluídas. A
 * garantia vem de {@link EngineDispatcher#retryFailed} retomar a partir do `currentNodeId` da
 * instância (a etapa falha), não do START — verificado registrando quais nós o dispatcher realmente
 * visitou durante o retry.
 */
class RetryIntegrationTest {

  private static final String GRAPH =
      """
            {"workflowKey":"k","name":"n","startNodeId":"start-1","nodes":[
              {"id":"start-1","type":"START","name":"Início","configuration":{}},
              {"id":"failing-1","type":"REST_SERVICE_TASK","name":"Falha","configuration":{}},
              {"id":"end-1","type":"END","name":"Fim","configuration":{}}
            ],"edges":[
              {"id":"e1","source":"start-1","target":"failing-1"},
              {"id":"e2","source":"failing-1","target":"end-1"}
            ]}
            """;

  private final WorkflowInstanceRepository instanceRepository =
      mock(WorkflowInstanceRepository.class);
  private final NodeInstanceRepository nodeInstanceRepository = mock(NodeInstanceRepository.class);
  private final WorkflowVariableRepository variableRepository =
      mock(WorkflowVariableRepository.class);
  private final WorkflowVersionRepository versionRepository = mock(WorkflowVersionRepository.class);
  private final AuditEntryRepository auditEntryRepository = mock(AuditEntryRepository.class);
  private final AuditService auditService =
      new AuditService(auditEntryRepository, new ObjectMapper());
  private final List<String> visitedNodeIds = new ArrayList<>();

  private NodeExecutor recordingExecutor() {
    return new NodeExecutor() {
      @Override
      public boolean supports(String nodeType) {
        return "REST_SERVICE_TASK".equals(nodeType);
      }

      @Override
      public NodeExecutionResult execute(
          WorkflowNode node,
          WorkflowInstance instance,
          NodeInstance nodeInstance,
          ExecutionContext context,
          List<WorkflowEdge> outgoingEdges) {
        visitedNodeIds.add(node.id());
        return NodeExecutionResult.completed(Map.of());
      }
    };
  }

  @Test
  void retryResumesFromFailedNodeNotFromStart() {
    UUID versionId = UUID.randomUUID();
    when(versionRepository.findById(versionId))
        .thenReturn(Optional.of(new WorkflowVersion(UUID.randomUUID(), 1, GRAPH, "alice")));

    WorkflowInstance instance = new WorkflowInstance(versionId, "biz", "alice");
    instance.start();
    instance.moveTo("failing-1");
    instance.fail();
    when(instanceRepository.findById(instance.getId())).thenReturn(Optional.of(instance));
    when(variableRepository.findById(instance.getId())).thenReturn(Optional.empty());
    when(nodeInstanceRepository.findFirstByWorkflowInstanceIdAndNodeIdOrderByAttemptDesc(
            instance.getId(), "failing-1"))
        .thenReturn(
            Optional.of(new NodeInstance(instance.getId(), "failing-1", "REST_SERVICE_TASK", 1)));

    EngineDispatcher dispatcher =
        new EngineDispatcher(
            versionRepository,
            instanceRepository,
            nodeInstanceRepository,
            variableRepository,
            List.of(new StartNodeExecutor(), new EndNodeExecutor(), recordingExecutor()),
            auditService,
            new ObjectMapper(),
            new WorkflowMetrics(new SimpleMeterRegistry()));

    dispatcher.retryFailed(instance.getId());

    assertThat(visitedNodeIds).containsExactly("failing-1");
    assertThat(instance.getStatus()).isEqualTo("COMPLETED");
  }

  @Test
  void retryOnlyAllowedFromFailedState() {
    WorkflowInstance instance = new WorkflowInstance(UUID.randomUUID(), null, "alice");
    instance.start();
    assertThatThrownBy(instance::retryFromFailed).isInstanceOf(IllegalStateException.class);

    instance.fail();
    instance.retryFromFailed();
    assertThat(instance.getStatus()).isEqualTo("RUNNING");
  }

  @Test
  void instanceServiceRetryRecordsAuditAction() {
    UUID versionId = UUID.randomUUID();
    when(versionRepository.findById(versionId))
        .thenReturn(Optional.of(new WorkflowVersion(UUID.randomUUID(), 1, GRAPH, "alice")));
    WorkflowInstance instance = new WorkflowInstance(versionId, "biz", "alice");
    instance.start();
    instance.moveTo("failing-1");
    instance.fail();
    when(instanceRepository.findById(instance.getId())).thenReturn(Optional.of(instance));
    when(variableRepository.findById(instance.getId())).thenReturn(Optional.empty());

    EngineDispatcher dispatcher =
        new EngineDispatcher(
            versionRepository,
            instanceRepository,
            nodeInstanceRepository,
            variableRepository,
            List.of(new StartNodeExecutor(), new EndNodeExecutor(), recordingExecutor()),
            auditService,
            new ObjectMapper(),
            new WorkflowMetrics(new SimpleMeterRegistry()));
    InstanceService service =
        new InstanceService(dispatcher, instanceRepository, nodeInstanceRepository, auditService);

    service.retry(instance.getId());

    verify(auditEntryRepository)
        .save(
            org.mockito.ArgumentMatchers.argThat(
                entry -> entry.getAction().equals(AuditAction.EXECUTION_RETRIED.name())));
  }
}
