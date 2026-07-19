package com.jouney.workflow.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import com.jouney.workflow.definition.WorkflowDefinition;
import com.jouney.workflow.definition.WorkflowDefinitionRepository;
import com.jouney.workflow.definition.WorkflowDefinitionService;
import com.jouney.workflow.publication.PublicationService;
import com.jouney.workflow.publication.WorkflowVersion;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * T033 — execução sequencial início→fim com histórico completo (research.md #7). Requer um
 * PostgreSQL rodando localmente (ver README.md — "Banco de dados"). Rodar com `mvn verify` (não
 * entra no `mvn test`). {@code @Transactional} desfaz os dados ao final (banco persistente real).
 */
@Tag("requires-postgres")
@SpringBootTest
@Transactional
class SequentialExecutionIntegrationTest {

  @Autowired private WorkflowDefinitionRepository definitionRepository;
  @Autowired private WorkflowDefinitionService definitionService;
  @Autowired private PublicationService publicationService;
  @Autowired private InstanceService instanceService;

  private static final String GRAPH =
      """
            {"workflowKey":"seq","name":"Sequencial","startNodeId":"start-1","nodes":[
              {"id":"start-1","type":"START","name":"Início","configuration":{}},
              {"id":"end-1","type":"END","name":"Fim","configuration":{}}
            ],"edges":[{"id":"e1","source":"start-1","target":"end-1"}]}
            """;

  @Test
  void instanceRunsAutomaticallyToCompletionWithHistory() {
    WorkflowDefinition definition =
        definitionRepository.save(new WorkflowDefinition("seq", "Sequencial", null, "alice"));
    definitionService.updateDraft(definition.getId(), "Sequencial", null, GRAPH);
    WorkflowVersion version = publicationService.publish(definition.getId(), "alice");

    WorkflowInstance instance = instanceService.start(version.getId(), "biz-1", "bob");

    assertThat(instance.getStatus()).isEqualTo("COMPLETED");
    assertThat(instanceService.history(instance.getId())).hasSize(2);
    assertThat(instanceService.history(instance.getId()))
        .extracting(NodeInstance::getNodeId)
        .containsExactly("start-1", "end-1");
  }
}
