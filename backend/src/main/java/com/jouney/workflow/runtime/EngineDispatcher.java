package com.jouney.workflow.runtime;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jouney.workflow.audit.AuditAction;
import com.jouney.workflow.audit.AuditService;
import com.jouney.workflow.definition.WorkflowGraph;
import com.jouney.workflow.definition.WorkflowGraph.WorkflowEdge;
import com.jouney.workflow.definition.WorkflowGraph.WorkflowNode;
import com.jouney.workflow.publication.WorkflowVersion;
import com.jouney.workflow.publication.WorkflowVersionRepository;
import com.jouney.workflow.shared.WorkflowMetrics;
import com.jouney.workflow.shared.error.NotFoundException;
import com.jouney.workflow.shared.logging.LoggingContext;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * T036/T043/T055/T062 — núcleo do motor (seção 6): localiza o próximo nó, executa o NodeExecutor
 * correspondente e avança até aguardar uma tarefa humana, finalizar ou falhar. Não mantém a
 * requisição HTTP aberta durante a execução (research.md #3).
 */
@Service
public class EngineDispatcher {

  private final WorkflowVersionRepository versionRepository;
  private final WorkflowInstanceRepository instanceRepository;
  private final NodeInstanceRepository nodeInstanceRepository;
  private final WorkflowVariableRepository variableRepository;
  private final List<NodeExecutor> executors;
  private final AuditService auditService;
  private final ObjectMapper objectMapper;
  private final WorkflowMetrics metrics;

  public EngineDispatcher(
      WorkflowVersionRepository versionRepository,
      WorkflowInstanceRepository instanceRepository,
      NodeInstanceRepository nodeInstanceRepository,
      WorkflowVariableRepository variableRepository,
      List<NodeExecutor> executors,
      AuditService auditService,
      ObjectMapper objectMapper,
      WorkflowMetrics metrics) {
    this.versionRepository = versionRepository;
    this.instanceRepository = instanceRepository;
    this.nodeInstanceRepository = nodeInstanceRepository;
    this.variableRepository = variableRepository;
    this.executors = executors;
    this.auditService = auditService;
    this.objectMapper = objectMapper;
    this.metrics = metrics;
  }

  @Transactional
  public WorkflowInstance start(UUID workflowVersionId, String businessKey, String startedBy) {
    WorkflowVersion version =
        versionRepository
            .findById(workflowVersionId)
            .orElseThrow(
                () -> new NotFoundException("Versão " + workflowVersionId + " não encontrada."));
    WorkflowGraph graph = parseGraph(version.getDefinitionJson());

    WorkflowInstance instance = new WorkflowInstance(workflowVersionId, businessKey, startedBy);
    instance.start();
    metrics.instanceStarted();
    instanceRepository.save(instance);
    variableRepository.save(new WorkflowVariable(instance.getId(), "{}"));

    LoggingContext.putWorkflowInstanceId(instance.getId().toString());
    LoggingContext.putBusinessKey(businessKey);

    runLoop(instance, graph, new ExecutionContext(Map.of()), graph.startNodeId());

    auditService.record(
        startedBy,
        AuditAction.EXECUTION_STARTED,
        "WORKFLOW_INSTANCE",
        instance.getId().toString(),
        Map.of());
    return instance;
  }

  /** T047 — retoma a execução a partir da etapa que estava em WAITING (chamado por US3/US6). */
  @Transactional
  public void resume(UUID instanceId) {
    WorkflowInstance instance =
        instanceRepository
            .findById(instanceId)
            .orElseThrow(
                () -> new NotFoundException("Instância " + instanceId + " não encontrada."));
    WorkflowVersion version =
        versionRepository
            .findById(instance.getWorkflowVersionId())
            .orElseThrow(() -> new NotFoundException("Versão não encontrada."));
    WorkflowGraph graph = parseGraph(version.getDefinitionJson());

    Map<String, Object> variables = loadVariables(instance.getId());
    String waitingNodeId = instance.getCurrentNodeId();
    instance.resume();

    String nextNodeId = singleOutgoingTarget(graph, waitingNodeId);
    runLoop(instance, graph, new ExecutionContext(variables), nextNodeId);
  }

  /** T070 — reprocessa só a etapa que falhou, sem repetir etapas já concluídas. */
  @Transactional
  public void retryFailed(UUID instanceId) {
    WorkflowInstance instance =
        instanceRepository
            .findById(instanceId)
            .orElseThrow(
                () -> new NotFoundException("Instância " + instanceId + " não encontrada."));
    WorkflowVersion version =
        versionRepository
            .findById(instance.getWorkflowVersionId())
            .orElseThrow(() -> new NotFoundException("Versão não encontrada."));
    WorkflowGraph graph = parseGraph(version.getDefinitionJson());

    Map<String, Object> variables = loadVariables(instance.getId());
    String failedNodeId = instance.getCurrentNodeId();
    instance.retryFromFailed();

    runLoop(instance, graph, new ExecutionContext(variables), failedNodeId);
  }

  private void runLoop(
      WorkflowInstance instance,
      WorkflowGraph graph,
      ExecutionContext context,
      String startNodeId) {
    String currentId = startNodeId;

    while (currentId != null) {
      WorkflowNode node = findNode(graph, currentId);
      NodeExecutor executor =
          executors.stream()
              .filter(e -> e.supports(node.type()))
              .findFirst()
              .orElseThrow(
                  () -> new IllegalStateException("Nenhum executor para o tipo " + node.type()));

      int attempt =
          nodeInstanceRepository
              .findFirstByWorkflowInstanceIdAndNodeIdOrderByAttemptDesc(instance.getId(), node.id())
              .map(ni -> ni.getAttempt() + 1)
              .orElse(1);
      NodeInstance nodeInstance =
          new NodeInstance(instance.getId(), node.id(), node.type(), attempt);
      instance.moveTo(node.id());
      LoggingContext.putNodeInstanceId(nodeInstance.getId().toString());
      nodeInstanceRepository.save(nodeInstance);

      List<WorkflowEdge> outgoingEdges =
          graph.edges().stream().filter(e -> e.source().equals(node.id())).toList();
      long nodeStartNanos = System.nanoTime();
      NodeExecutionResult result =
          executor.execute(node, instance, nodeInstance, context, outgoingEdges);
      metrics.nodeExecutionTime(
          node.type(), java.time.Duration.ofNanos(System.nanoTime() - nodeStartNanos));

      switch (result.outcome()) {
        case COMPLETED -> {
          context.putAll(result.outputVariables());
          nodeInstance.complete(toJson(result.outputVariables()));
          persistVariables(instance.getId(), context);

          String nextId =
              result.nextNodeId() != null
                  ? result.nextNodeId()
                  : singleOutgoingTarget(graph, node.id());
          if (nextId == null) {
            instance.complete();
            metrics.instanceCompleted();
            instanceRepository.save(instance);
            auditService.record(
                instance.getStartedBy(),
                com.jouney.workflow.audit.AuditAction.EXECUTION_COMPLETED,
                "WORKFLOW_INSTANCE",
                instance.getId().toString(),
                Map.of());
            return;
          }
          currentId = nextId;
        }
        case WAITING -> {
          nodeInstance.waitState();
          instance.waitFor(node.id());
          instanceRepository.save(instance);
          return;
        }
        case FAILED -> {
          nodeInstance.fail(
              toJson(Map.of("message", result.errorMessage(), "category", result.errorCategory())));
          instance.fail();
          metrics.instanceFailed();
          instanceRepository.save(instance);
          return;
        }
      }
    }
  }

  private String singleOutgoingTarget(WorkflowGraph graph, String nodeId) {
    List<WorkflowEdge> outgoing =
        graph.edges().stream().filter(e -> e.source().equals(nodeId)).toList();
    return outgoing.isEmpty() ? null : outgoing.get(0).target();
  }

  private WorkflowNode findNode(WorkflowGraph graph, String nodeId) {
    return graph.nodes().stream()
        .filter(n -> n.id().equals(nodeId))
        .findFirst()
        .orElseThrow(
            () -> new IllegalStateException("Nó " + nodeId + " não existe no grafo publicado."));
  }

  private WorkflowGraph parseGraph(String definitionJson) {
    try {
      return objectMapper.readValue(definitionJson, WorkflowGraph.class);
    } catch (Exception e) {
      throw new IllegalStateException("Definição publicada inválida: " + e.getMessage(), e);
    }
  }

  private Map<String, Object> loadVariables(UUID instanceId) {
    Optional<WorkflowVariable> variable = variableRepository.findById(instanceId);
    if (variable.isEmpty()) {
      return Map.of();
    }
    try {
      return objectMapper.readValue(
          variable.get().getVariablesJson(), new TypeReference<Map<String, Object>>() {});
    } catch (Exception e) {
      return Map.of();
    }
  }

  private void persistVariables(UUID instanceId, ExecutionContext context) {
    variableRepository
        .findById(instanceId)
        .ifPresent(variable -> variable.update(toJson(context.asMap())));
  }

  private String toJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception e) {
      return "{}";
    }
  }
}
