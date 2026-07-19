package com.jouney.workflow.publication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jouney.workflow.definition.WorkflowDefinition;
import com.jouney.workflow.definition.WorkflowDefinitionRepository;
import com.jouney.workflow.definition.WorkflowDefinitionService;
import com.jouney.workflow.shared.error.ValidationFailedException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * research.md #7 — integração com PostgreSQL real. Requer um PostgreSQL rodando localmente na
 * URL/credenciais de `application.yml` (ver README.md — "Banco de dados"); as migrações Flyway
 * rodam automaticamente ao subir o contexto. Rodar com `mvn verify` (não entra no `mvn test`).
 * {@code @Transactional} aqui desfaz os dados de cada teste ao final (banco persistente real, sem
 * container efêmero), evitando colisão de `workflow_key` entre execuções.
 */
@Tag("requires-postgres")
@SpringBootTest
@Transactional
class PublicationIntegrationTest {

  @Autowired private WorkflowDefinitionRepository definitionRepository;
  @Autowired private WorkflowDefinitionService definitionService;
  @Autowired private PublicationService publicationService;
  @Autowired private WorkflowVersionRepository versionRepository;

  private static final String VALID_GRAPH =
      """
            {"workflowKey":"k","name":"n","startNodeId":"start-1","nodes":[
              {"id":"start-1","type":"START","name":"Início","configuration":{}},
              {"id":"end-1","type":"END","name":"Fim","configuration":{}}
            ],"edges":[{"id":"e1","source":"start-1","target":"end-1"}]}
            """;

  @Test
  void publishingCreatesImmutableActiveVersion() {
    WorkflowDefinition definition =
        definitionRepository.save(new WorkflowDefinition("k1", "n", null, "alice"));
    definitionService.updateDraft(definition.getId(), "n", null, VALID_GRAPH);

    WorkflowVersion version = publicationService.publish(definition.getId(), "alice");

    assertThat(version.getVersionNumber()).isEqualTo(1);
    assertThat(version.isActive()).isTrue();
    assertThat(versionRepository.findById(version.getId())).isPresent();
  }

  @Test
  void publishingWithInvalidGraphIsBlockedWithAllProblems() {
    WorkflowDefinition definition =
        definitionRepository.save(new WorkflowDefinition("k2", "n", null, "alice"));
    definitionService.updateDraft(definition.getId(), "n", null, "{\"nodes\":[]}");

    assertThatThrownBy(() -> publicationService.publish(definition.getId(), "alice"))
        .isInstanceOf(ValidationFailedException.class)
        .satisfies(e -> assertThat(((ValidationFailedException) e).getProblems()).isNotEmpty());
  }
}
