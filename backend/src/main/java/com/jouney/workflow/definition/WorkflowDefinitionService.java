package com.jouney.workflow.definition;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jouney.workflow.shared.error.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkflowDefinitionService {

  private final WorkflowDefinitionRepository repository;
  private final GraphValidator graphValidator;
  private final ObjectMapper objectMapper;

  public WorkflowDefinitionService(
      WorkflowDefinitionRepository repository,
      GraphValidator graphValidator,
      ObjectMapper objectMapper) {
    this.repository = repository;
    this.graphValidator = graphValidator;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public WorkflowDefinition create(
      String workflowKey, String name, String description, String createdBy) {
    WorkflowDefinition definition =
        new WorkflowDefinition(workflowKey, name, description, createdBy);
    return repository.save(definition);
  }

  public WorkflowDefinition get(UUID id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Workflow " + id + " não encontrado."));
  }

  public List<WorkflowDefinition> list() {
    return repository.findAll();
  }

  @Transactional
  public WorkflowDefinition updateDraft(
      UUID id, String name, String description, String graphJson) {
    WorkflowDefinition definition = get(id);
    definition.updateDraft(name, description, graphJson);
    return definition;
  }

  /** FR-004: retorna a lista completa de problemas — vazia significa "pronto para publicar". */
  public List<String> validate(UUID id) {
    WorkflowDefinition definition = get(id);
    return validateGraphJson(definition.getGraphJson());
  }

  public List<String> validateGraphJson(String graphJson) {
    try {
      WorkflowGraph graph = objectMapper.readValue(graphJson, WorkflowGraph.class);
      return graphValidator.validate(graph);
    } catch (Exception e) {
      return List.of("O grafo do workflow não é um JSON válido: " + e.getMessage());
    }
  }
}
