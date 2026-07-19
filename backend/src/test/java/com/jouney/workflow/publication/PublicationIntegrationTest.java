package com.jouney.workflow.publication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jouney.workflow.definition.WorkflowDefinition;
import com.jouney.workflow.definition.WorkflowDefinitionRepository;
import com.jouney.workflow.shared.error.ValidationFailedException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * research.md #7 — integração com PostgreSQL real via Testcontainers. Requer Docker disponível;
 * neste ambiente de desenvolvimento o Docker ainda não estava configurado no momento em que este
 * teste foi escrito (ver relato de implementação).
 */
@Testcontainers
@SpringBootTest
class PublicationIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired private WorkflowDefinitionRepository definitionRepository;
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
    definition.updateDraft("n", null, VALID_GRAPH);

    WorkflowVersion version = publicationService.publish(definition.getId(), "alice");

    assertThat(version.getVersionNumber()).isEqualTo(1);
    assertThat(version.isActive()).isTrue();
    assertThat(versionRepository.findById(version.getId())).isPresent();
  }

  @Test
  void publishingWithInvalidGraphIsBlockedWithAllProblems() {
    WorkflowDefinition definition =
        definitionRepository.save(new WorkflowDefinition("k2", "n", null, "alice"));
    definition.updateDraft("n", null, "{\"nodes\":[]}");

    assertThatThrownBy(() -> publicationService.publish(definition.getId(), "alice"))
        .isInstanceOf(ValidationFailedException.class)
        .satisfies(e -> assertThat(((ValidationFailedException) e).getProblems()).isNotEmpty());
  }
}
