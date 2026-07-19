package com.jouney.workflow.humantask;

import static org.assertj.core.api.Assertions.assertThat;

import com.jouney.workflow.definition.WorkflowDefinition;
import com.jouney.workflow.definition.WorkflowDefinitionRepository;
import com.jouney.workflow.definition.WorkflowDefinitionService;
import com.jouney.workflow.publication.PublicationService;
import com.jouney.workflow.publication.WorkflowVersion;
import com.jouney.workflow.runtime.InstanceService;
import com.jouney.workflow.runtime.WorkflowInstance;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * T042 — retomada automática da execução após concluir a tarefa (US3). Requer um PostgreSQL rodando
 * localmente (ver README.md — "Banco de dados"). Rodar com `mvn verify` (não entra no `mvn test`).
 * {@code @Transactional} desfaz os dados ao final (banco persistente real).
 */
@Tag("requires-postgres")
@SpringBootTest
@Transactional
class HumanTaskResumeIntegrationTest {

  @Autowired private WorkflowDefinitionRepository definitionRepository;
  @Autowired private WorkflowDefinitionService definitionService;
  @Autowired private PublicationService publicationService;
  @Autowired private InstanceService instanceService;
  @Autowired private HumanTaskService humanTaskService;
  @Autowired private HumanTaskRepository humanTaskRepository;

  private static final String GRAPH =
      """
            {"workflowKey":"ht","name":"Com tarefa","startNodeId":"start-1","nodes":[
              {"id":"start-1","type":"START","name":"Início","configuration":{}},
              {"id":"task-1","type":"USER_TASK","name":"Preencher dados","configuration":{
                "formSchema":{"title":"Dados","fields":[{"key":"name","label":"Nome","type":"text","required":true}]},
                "assignment":{"type":"INITIATOR"}
              }},
              {"id":"end-1","type":"END","name":"Fim","configuration":{}}
            ],"edges":[
              {"id":"e1","source":"start-1","target":"task-1"},
              {"id":"e2","source":"task-1","target":"end-1"}
            ]}
            """;

  @Test
  void completingTheTaskResumesExecutionToCompletion() {
    WorkflowDefinition definition =
        definitionRepository.save(new WorkflowDefinition("ht", "Com tarefa", null, "alice"));
    definitionService.updateDraft(definition.getId(), "Com tarefa", null, GRAPH);
    WorkflowVersion version = publicationService.publish(definition.getId(), "alice");

    WorkflowInstance instance = instanceService.start(version.getId(), "biz-1", "alice");
    assertThat(instance.getStatus()).isEqualTo("WAITING");

    List<HumanTask> pending = humanTaskRepository.findPendingForUser("alice", List.of("__none__"));
    assertThat(pending).hasSize(1);

    humanTaskService.claim(pending.get(0).getId(), "alice");
    humanTaskService.complete(pending.get(0).getId(), Map.of("name", "Alice"));

    WorkflowInstance resumed = instanceService.get(instance.getId());
    assertThat(resumed.getStatus()).isEqualTo("COMPLETED");
  }
}
