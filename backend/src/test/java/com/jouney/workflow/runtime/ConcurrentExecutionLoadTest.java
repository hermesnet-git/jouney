package com.jouney.workflow.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import com.jouney.workflow.definition.WorkflowDefinition;
import com.jouney.workflow.definition.WorkflowDefinitionRepository;
import com.jouney.workflow.publication.PublicationService;
import com.jouney.workflow.publication.WorkflowVersion;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * T080 — SC-008: algumas dezenas de execuções simultâneas sem degradação perceptível. Dispara 50
 * execuções em paralelo contra a mesma versão publicada e confirma que todas concluem sem erro.
 * Requer Docker (Testcontainers), como os demais testes de integração deste projeto.
 */
@Testcontainers
@SpringBootTest
class ConcurrentExecutionLoadTest {

  private static final int CONCURRENT_EXECUTIONS = 50;

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
  @Autowired private InstanceService instanceService;

  private static final String GRAPH =
      """
            {"workflowKey":"load","name":"Carga","startNodeId":"start-1","nodes":[
              {"id":"start-1","type":"START","name":"Início","configuration":{}},
              {"id":"end-1","type":"END","name":"Fim","configuration":{}}
            ],"edges":[{"id":"e1","source":"start-1","target":"end-1"}]}
            """;

  @Test
  void fiftyConcurrentExecutionsAllCompleteWithoutErrors() throws Exception {
    WorkflowDefinition definition =
        definitionRepository.save(new WorkflowDefinition("load", "Carga", null, "alice"));
    definition.updateDraft("Carga", null, GRAPH);
    WorkflowVersion version = publicationService.publish(definition.getId(), "alice");

    ExecutorService pool = Executors.newFixedThreadPool(CONCURRENT_EXECUTIONS);
    try {
      List<Callable<WorkflowInstance>> tasks =
          java.util.stream.IntStream.range(0, CONCURRENT_EXECUTIONS)
              .mapToObj(
                  i ->
                      (Callable<WorkflowInstance>)
                          () -> instanceService.start(version.getId(), "biz-" + i, "load-test"))
              .toList();

      List<Future<WorkflowInstance>> futures = pool.invokeAll(tasks);

      for (Future<WorkflowInstance> future : futures) {
        WorkflowInstance instance = future.get();
        assertThat(instance.getStatus()).isEqualTo("COMPLETED");
      }
    } finally {
      pool.shutdown();
    }
  }
}
