---

description: "Task list template for feature implementation"
---

# Tasks: Plataforma de Workflows (MVP)

**Input**: Design documents from `/specs/001-workflow-platform-mvp/`

**Prerequisites**: plan.md, spec.md, data-model.md, contracts/api.md, research.md, quickstart.md (todos presentes)

**Tests**: Incluídos — decisão técnica registrada em `research.md` #7 (herdada
da seção 15 do planejamento.md); a constitution não exige testes como gate,
mas a decisão de arquitetura já tomada os inclui explicitamente.

**Organization**: Tarefas agrupadas por user story (US1-US6, spec.md) para
permitir implementação e teste independentes de cada uma.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Pode rodar em paralelo (arquivos diferentes, sem dependência)
- **[Story]**: A qual user story a tarefa pertence (US1-US6)
- Caminhos de arquivo exatos em cada descrição, conforme a Structure Decision de `plan.md`

## Path Conventions

- Backend: `backend/src/main/java/com/jouney/workflow/<módulo>/`, testes em `backend/src/test/java/com/jouney/workflow/`
- Frontend: `frontend/src/<área>/`, testes em `frontend/tests/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Inicialização dos dois projetos (backend/frontend) e pipeline

- [ ] T001 Criar esqueleto do backend Spring Boot modular em `backend/` com os 9 pacotes de módulo vazios (definition, publication, runtime, humantask, connector, expression, identity, audit, shared) conforme Structure Decision de `plan.md`
- [ ] T002 Criar esqueleto do frontend React + React Flow em `frontend/` com a árvore `src/app`, `workflow-designer/*`, `workflow-management`, `workflow-runtime`, `task-inbox`, `connectors`, `shared`, `api`
- [ ] T003 [P] Configurar PostgreSQL local + Flyway em `backend/src/main/resources/db/migration/`
- [ ] T004 [P] Configurar lint/format do backend (Checkstyle/Spotless) em `backend/`
- [ ] T005 [P] Configurar lint/format do frontend (ESLint/Prettier) em `frontend/`
- [ ] T006 Configurar pipeline de CI (build + lint + test dos dois projetos)

**Checkpoint**: Dois projetos versionados, buildando e com lint configurado.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Infraestrutura que TODAS as user stories dependem

**⚠️ CRITICAL**: Nenhuma user story pode começar antes desta fase estar completa

- [ ] T007 Implementar autenticação OpenID Connect (resource server) em `backend/src/main/java/com/jouney/workflow/identity/`
- [ ] T008 Implementar autorização por papel (WORKFLOW_DESIGNER, WORKFLOW_PUBLISHER, WORKFLOW_OPERATOR, TASK_USER, PLATFORM_ADMIN — FR-022) em `backend/src/main/java/com/jouney/workflow/identity/`
- [ ] T009 [P] Criar tipos base e formato único de erro (`code`/`message`/`details[]`, contracts/api.md) em `backend/src/main/java/com/jouney/workflow/shared/`
- [ ] T010 [P] Migração Flyway: tabelas `workflow_definition`, `workflow_version` em `backend/src/main/resources/db/migration/`
- [ ] T011 [P] Migração Flyway: tabelas `workflow_instance`, `node_instance`, `workflow_variable` em `backend/src/main/resources/db/migration/`
- [ ] T012 [P] Migração Flyway: tabelas `human_task`, `connector_definition`, `execution_event`, `outbox_event` em `backend/src/main/resources/db/migration/`
- [ ] T013 Implementar serviço de auditoria (gravação, FR-021) em `backend/src/main/java/com/jouney/workflow/audit/`
- [ ] T014 [P] Configurar logging estruturado JSON + propagação de correlationId/workflowInstanceId/nodeInstanceId/businessKey (seção 14) em `backend/src/main/java/com/jouney/workflow/shared/`
- [ ] T015 [P] Criar shell/layout/rotas da aplicação em `frontend/src/app/`
- [ ] T016 [P] Criar client HTTP base (token OIDC, tratamento de erro único) em `frontend/src/api/`

**Checkpoint**: Fundação pronta — implementação das user stories pode começar.

---

## Phase 3: User Story 1 - Desenhar e publicar um workflow (Priority: P1) 🎯 MVP

**Goal**: Designer cria, conecta, configura, valida e publica um workflow.

**Independent Test**: Desenhar um workflow simples (início → uma etapa → fim), publicá-lo e confirmar via `GET /api/workflows/{id}/versions` que uma versão imutável ficou disponível — sem depender de execução.

### Tests for User Story 1

- [ ] T017 [P] [US1] Teste unitário do validador de grafo (início ausente, nó desconectado, gateway sem condição, tarefa humana sem formulário, etapa inalcançável, fim ausente — FR-004) em `backend/src/test/java/com/jouney/workflow/definition/GraphValidatorTest.java`
- [ ] T018 [P] [US1] Teste de integração: publicar cria uma Workflow Version imutável e bloqueia nova publicação com problemas de validação em `backend/src/test/java/com/jouney/workflow/publication/PublicationIntegrationTest.java`
- [ ] T019 [P] [US1] Teste de componente: serialização do grafo React Flow ↔ JSON (seção 5) em `frontend/tests/workflow-designer/serialization.test.tsx`

### Implementation for User Story 1

- [ ] T020 [P] [US1] Modelo + repositório de Workflow Definition em `backend/src/main/java/com/jouney/workflow/definition/`
- [ ] T021 [P] [US1] Modelo + repositório de Workflow Version em `backend/src/main/java/com/jouney/workflow/publication/`
- [ ] T022 [US1] Validador de grafo (FR-004) em `backend/src/main/java/com/jouney/workflow/definition/GraphValidator.java` (depende de T020)
- [ ] T023 [US1] Endpoints `POST/GET/PUT /api/workflows` em `backend/src/main/java/com/jouney/workflow/definition/WorkflowController.java` (depende de T020)
- [ ] T024 [US1] Endpoint `POST /api/workflows/{id}/validate` em `backend/src/main/java/com/jouney/workflow/definition/WorkflowController.java` (depende de T022)
- [ ] T025 [US1] Endpoints `POST /api/workflows/{id}/publish` e `GET .../versions` em `backend/src/main/java/com/jouney/workflow/publication/PublicationController.java` (depende de T021, T022)
- [ ] T026 [P] [US1] Canvas + palette (Início, Fim, Tarefa de Usuário, REST, Gateway), com navegação por teclado entre nós/paleta e contraste suficiente (constitution II — não é opcional nem adiável para Polish) em `frontend/src/workflow-designer/canvas/`, `frontend/src/workflow-designer/palette/`
- [ ] T027 [P] [US1] Componentes de nó customizados, com foco visível e rótulos acessíveis (constitution II) em `frontend/src/workflow-designer/nodes/`
- [ ] T028 [US1] Painel de propriedades para os tipos genéricos do MVP (Início, Fim, Tarefa de Usuário, Gateway), com campos navegáveis por teclado (constitution II); campos específicos do conector REST (timeout, retry, mapeamento) ficam em T058 (US4), quando o módulo Connector existir em `frontend/src/workflow-designer/properties-panel/` (depende de T027)
- [ ] T029 [US1] Serialização grafo ↔ JSON em `frontend/src/workflow-designer/serialization/`
- [ ] T030 [US1] Painel de validação (consome `/validate`) em `frontend/src/workflow-designer/validation/` (depende de T024)
- [ ] T031 [US1] UI de listagem/rascunho/publicação em `frontend/src/workflow-management/` (depende de T023, T025)

**Checkpoint**: US1 completa e testável de forma independente (SC-001, SC-002).

---

## Phase 4: User Story 2 - Executar um workflow do início ao fim (Priority: P1)

**Goal**: Operador inicia uma execução e ela avança sozinha até concluir, com histórico.

**Independent Test**: Publicar um workflow só com etapas automáticas (início, etapas automáticas, fim), iniciar uma execução e observá-la chegar ao fim sozinha, com histórico completo.

### Tests for User Story 2

- [ ] T032 [P] [US2] Teste unitário das transições de estado da instância (CREATED→RUNNING→...→COMPLETED/FAILED/CANCELLED, seção 6) em `backend/src/test/java/com/jouney/workflow/runtime/InstanceStateMachineTest.java`
- [ ] T033 [P] [US2] Teste de integração (Testcontainers, Postgres real): execução sequencial início→fim com histórico completo em `backend/src/test/java/com/jouney/workflow/runtime/SequentialExecutionIntegrationTest.java`

### Implementation for User Story 2

- [ ] T034 [P] [US2] Modelos + repositórios de Workflow Instance e Step Instance em `backend/src/main/java/com/jouney/workflow/runtime/`
- [ ] T035 [P] [US2] Modelo + repositório de Workflow Variable em `backend/src/main/java/com/jouney/workflow/runtime/`
- [ ] T036 [US2] Dispatcher do motor (localiza próximo nó, executa START/END) em `backend/src/main/java/com/jouney/workflow/runtime/EngineDispatcher.java` (depende de T034)
- [ ] T037 [US2] Endpoint `POST /api/workflow-versions/{versionId}/instances` em `backend/src/main/java/com/jouney/workflow/runtime/InstanceController.java` (depende de T036)
- [ ] T038 [US2] Endpoints `GET /api/instances/{id}` e `GET /api/instances/{id}/history` em `backend/src/main/java/com/jouney/workflow/runtime/InstanceController.java` (depende de T034)
- [ ] T039 [US2] Optimistic locking (`lock_version`) nas atualizações de instância em `backend/src/main/java/com/jouney/workflow/runtime/` (depende de T034)
- [ ] T040 [P] [US2] Execution viewer (status + histórico, reutilizando React Flow) em `frontend/src/workflow-runtime/` (depende de T038)

**Checkpoint**: US1+US2 → fluxo totalmente automático demonstrável de ponta a ponta (SC-003).

---

## Phase 5: User Story 3 - Concluir uma tarefa humana (Priority: P2)

**Goal**: Usuário assume, preenche e conclui uma tarefa humana; a execução retoma sozinha.

**Independent Test**: Publicar um workflow com uma etapa de tarefa humana, iniciar uma execução, assumir e concluir a tarefa, e confirmar que a execução retoma sozinha.

### Tests for User Story 3

- [ ] T041 [P] [US3] Teste unitário de validação de formulário (campo obrigatório ausente, FR-012) em `backend/src/test/java/com/jouney/workflow/humantask/FormValidationTest.java`
- [ ] T042 [P] [US3] Teste de integração: retomada automática da execução após concluir a tarefa em `backend/src/test/java/com/jouney/workflow/humantask/HumanTaskResumeIntegrationTest.java`

### Implementation for User Story 3

- [ ] T043 [US3] Tipo de nó USER_TASK no dispatcher (cria Human Task e coloca a instância em WAITING) em `backend/src/main/java/com/jouney/workflow/runtime/EngineDispatcher.java` (depende de T036)
- [ ] T044 [P] [US3] Modelo + repositório de Human Task em `backend/src/main/java/com/jouney/workflow/humantask/`
- [ ] T045 [US3] Claim exclusivo (concorrência entre membros do grupo) em `backend/src/main/java/com/jouney/workflow/humantask/` (depende de T044)
- [ ] T046 [US3] Validação de `form_data_json` contra `form_schema_json` em `backend/src/main/java/com/jouney/workflow/humantask/FormValidator.java`
- [ ] T047 [US3] Comando de retomada da execução ao concluir a tarefa em `backend/src/main/java/com/jouney/workflow/runtime/` (depende de T043, T046)
- [ ] T048 [US3] Endpoints `GET /api/tasks`, `GET /api/tasks/{id}`, `POST .../claim`, `POST .../complete` em `backend/src/main/java/com/jouney/workflow/humantask/TaskController.java` (depende de T045, T046, T047)
- [ ] T049 [P] [US3] Renderizador dinâmico de formulário (Text/Number/Date/Boolean/Select/Textarea, seção 9) em `frontend/src/task-inbox/`
- [ ] T050 [US3] UI de inbox de tarefas (listar, assumir, concluir) em `frontend/src/task-inbox/` (depende de T049, T048)

**Checkpoint**: US1+US2+US3 → workflow com etapa humana funcional (SC-004).

---

## Phase 6: User Story 4 - Chamar um serviço externo (Priority: P2)

**Goal**: Etapa de chamada REST com mapeamento, timeout e retry automático.

**Independent Test**: Publicar um workflow com uma etapa de chamada externa apontando para um serviço de teste, executá-lo e confirmar que o resultado chega às variáveis da execução.

### Tests for User Story 4

- [ ] T051 [P] [US4] Teste unitário de mapeamento de entrada/saída e política de retry (maxAttempts/initialDelay/backoffMultiplier, seção 12) em `backend/src/test/java/com/jouney/workflow/connector/RestConnectorExecutorTest.java`
- [ ] T052 [P] [US4] Teste de integração: chamada REST mockada com sucesso e com falha temporária seguida de nova tentativa automática em `backend/src/test/java/com/jouney/workflow/connector/RestConnectorIntegrationTest.java`

### Implementation for User Story 4

- [ ] T053 [P] [US4] Modelo + repositório de Connector (config sem credencial inline, FR-014) em `backend/src/main/java/com/jouney/workflow/connector/`
- [ ] T054 [US4] Interface `ConnectorExecutor` + `RestConnectorExecutor` em `backend/src/main/java/com/jouney/workflow/connector/` (depende de T053)
- [ ] T055 [US4] Tipo de nó REST_SERVICE_TASK no dispatcher (timeout, retry, chave de idempotência `executionId`/`nodeInstanceId`/`attempt`, e armazenamento do resultado da chamada em Workflow Variable — FR-015) em `backend/src/main/java/com/jouney/workflow/runtime/EngineDispatcher.java` (depende de T054, T036)
- [ ] T056 [US4] Classificação de erro (temporário/negócio/configuração/não recuperável, seção 12) em `backend/src/main/java/com/jouney/workflow/runtime/`
- [ ] T057 [US4] Endpoints `POST/GET/PUT /api/connectors` e `POST .../test` em `backend/src/main/java/com/jouney/workflow/connector/ConnectorController.java` (depende de T053)
- [ ] T058 [P] [US4] UI de cadastro/teste de conector em `frontend/src/connectors/`, e extensão do painel de propriedades (T028) com os campos específicos do conector REST (timeout, retry, mapeamento de entrada/saída) em `frontend/src/workflow-designer/properties-panel/` (depende de T057, T028)

**Checkpoint**: US1-US4 → workflow com integração externa real e resiliente (SC-005).

---

## Phase 7: User Story 5 - Tomar uma decisão e publicar um evento (Priority: P3)

**Goal**: Etapa de decisão ramifica a execução e um caminho publica um evento externo.

**Independent Test**: Publicar um workflow com uma etapa de decisão de dois caminhos e confirmar que cada caminho é seguido conforme a condição avaliada, incluindo a publicação do evento no caminho aplicável.

### Tests for User Story 5

- [ ] T059 [P] [US5] Teste unitário do avaliador de condições (linguagem declarativa limitada, seção 3.6) em `backend/src/test/java/com/jouney/workflow/expression/ExpressionEvaluatorTest.java`
- [ ] T060 [P] [US5] Teste de integração: caminho de decisão publica evento via outbox (consistência transacional) em `backend/src/test/java/com/jouney/workflow/runtime/GatewayOutboxIntegrationTest.java`

### Implementation for User Story 5

- [ ] T061 [P] [US5] Avaliador de expressões declarativas (`customer.age >= 18`, etc.) em `backend/src/main/java/com/jouney/workflow/expression/ExpressionEvaluator.java`
- [ ] T062 [US5] Tipo de nó EXCLUSIVE_GATEWAY no dispatcher em `backend/src/main/java/com/jouney/workflow/runtime/EngineDispatcher.java` (depende de T061, T036)
- [ ] T063 [P] [US5] Modelo + repositório de Outbox Event e publicador assíncrono em `backend/src/main/java/com/jouney/workflow/runtime/`
- [ ] T064 [US5] `KafkaPublishConnectorExecutor`/`RabbitMqPublishConnectorExecutor` em `backend/src/main/java/com/jouney/workflow/connector/` (depende de T054)
- [ ] T065 [US5] Ligar o caminho de publicação de evento do gateway ao outbox (mesma transação, FR-018) em `backend/src/main/java/com/jouney/workflow/runtime/` (depende de T062, T063, T064)
- [ ] T066 [P] [US5] UI de configuração de condições do gateway em `frontend/src/workflow-designer/properties-panel/`

**Checkpoint**: US1-US5 → fluxo de ponta a ponta completo, replicando a prova de conceito da seção 19 do planejamento.md.

---

## Phase 8: User Story 6 - Operar as execuções em produção (Priority: P3)

**Goal**: Operador visualiza, cancela, força retry e consulta auditoria/métricas.

**Independent Test**: Forçar uma falha em uma etapa, cancelar uma execução, forçar nova tentativa em outra, e confirmar que todas as ações aparecem na trilha de auditoria.

### Tests for User Story 6

- [ ] T067 [P] [US6] Teste de integração: retry reprocessa só a etapa falha, sem repetir etapas concluídas em `backend/src/test/java/com/jouney/workflow/runtime/RetryIntegrationTest.java`
- [ ] T068 [P] [US6] Teste de integração: cancelamento de execução em andamento em `backend/src/test/java/com/jouney/workflow/runtime/CancelIntegrationTest.java`

### Implementation for User Story 6

- [ ] T069 [US6] Endpoint `POST /api/instances/{id}/cancel` em `backend/src/main/java/com/jouney/workflow/runtime/InstanceController.java` (depende de T038)
- [ ] T070 [US6] Endpoint `POST /api/instances/{id}/retry` (reprocessa a partir da etapa falha) em `backend/src/main/java/com/jouney/workflow/runtime/InstanceController.java` (depende de T038, T056)
- [ ] T071 [US6] Instrumentar publish/start/complete/cancel/retry com chamadas ao serviço de auditoria (T013) nos módulos publication/runtime/humantask
- [ ] T072 [US6] Endpoint `GET /api/audit` (filtrável por entidade/ator/período) em `backend/src/main/java/com/jouney/workflow/audit/AuditController.java` (depende de T013)
- [ ] T073 [P] [US6] Dashboard operacional de instâncias (listar, cancelar, retry) em `frontend/src/workflow-runtime/` (depende de T069, T070)
- [ ] T074 [P] [US6] UI de trilha de auditoria em `frontend/src/shared/` (depende de T072)

**Checkpoint**: Todas as 6 user stories funcionais e operáveis (SC-006, SC-007).

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Itens que atravessam todas as user stories

- [ ] T075 [P] Auditoria final de acessibilidade em todas as telas (designer T026-T028, inbox T050, dashboard T073) — confirma o que já foi implementado desde US1, não é a primeira nem única tarefa de acessibilidade (constitution II) em `frontend/src/`
- [ ] T076 [P] Métricas (instâncias iniciadas/concluídas/com erro, tempos médios por workflow/nó, erros por conector, retentativas — seção 14) em `backend/src/main/java/com/jouney/workflow/shared/`
- [ ] T077 [P] Testes de arquitetura (impedir dependência indevida entre os 9 módulos) em `backend/src/test/java/com/jouney/workflow/architecture/`
- [ ] T078 Rodar `quickstart.md` de ponta a ponta e corrigir divergências encontradas
- [ ] T079 [P] README de setup local (backend + frontend + banco) na raiz do repositório, incluindo confirmação de que a interface é só PT-BR sem framework de i18n (FR-023)
- [ ] T080 [P] Teste de carga/concorrência validando SC-008 (algumas dezenas de execuções simultâneas sem degradação perceptível nos endpoints de instância/inbox) em `backend/src/test/java/com/jouney/workflow/runtime/ConcurrentExecutionLoadTest.java`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sem dependências — pode começar imediatamente
- **Foundational (Phase 2)**: depende do Setup — BLOQUEIA todas as user stories
- **US1, US2 (Phase 3-4, P1)**: dependem só do Foundational; US2 depende do dispatcher base que US1 não usa, então podem ser feitas em paralelo por equipes diferentes, mas US2 sozinha só demonstra valor pleno depois que US1 existe (precisa de algo publicado para executar)
- **US3 (Phase 5, P2)**: depende do dispatcher criado em US2 (T036)
- **US4 (Phase 6, P2)**: depende do dispatcher criado em US2 (T036); independente de US3
- **US5 (Phase 7, P3)**: depende do dispatcher (T036) e do Connector base (T054) de US4
- **US6 (Phase 8, P3)**: depende dos endpoints de instância de US2 (T038) e da classificação de erro de US4 (T056)
- **Polish (Phase 9)**: depende de todas as user stories desejadas estarem completas

### User Story Dependencies

- **US1 (P1)**: nenhuma dependência de outra story
- **US2 (P1)**: precisa de uma versão publicada para demonstrar valor pleno (usa saída de US1), mas seu código (motor, instância) não depende do código de US1
- **US3 (P2)**: depende do dispatcher de US2 (T036)
- **US4 (P2)**: depende do dispatcher de US2 (T036); independente de US3
- **US5 (P3)**: depende do dispatcher de US2 e do Connector de US4
- **US6 (P3)**: depende dos endpoints de instância de US2 e da classificação de erro de US4

### Parallel Opportunities

- Todas as tarefas [P] do Setup podem rodar em paralelo
- Todas as tarefas [P] do Foundational podem rodar em paralelo (dentro da Phase 2)
- Depois do Foundational: US1 e a parte de modelos/testes de US2 podem começar em paralelo (times diferentes), já que só convergem no dispatcher (T036)
- Dentro de cada user story, testes marcados [P] rodam em paralelo entre si, e tarefas de frontend marcadas [P] rodam em paralelo com as de backend quando não há dependência direta

---

## Parallel Example: User Story 1

```bash
# Testes de US1 em paralelo:
Task: "Teste unitário do validador de grafo em backend/src/test/java/com/jouney/workflow/definition/GraphValidatorTest.java"
Task: "Teste de integração de publicação em backend/src/test/java/com/jouney/workflow/publication/PublicationIntegrationTest.java"
Task: "Teste de serialização do grafo em frontend/tests/workflow-designer/serialization.test.tsx"

# Modelos de US1 em paralelo:
Task: "Modelo + repositório de Workflow Definition em backend/src/main/java/com/jouney/workflow/definition/"
Task: "Modelo + repositório de Workflow Version em backend/src/main/java/com/jouney/workflow/publication/"
```

---

## Implementation Strategy

### MVP First (User Story 1 + User Story 2)

1. Completar Phase 1: Setup
2. Completar Phase 2: Foundational (CRITICAL — bloqueia todas as stories)
3. Completar Phase 3: User Story 1
4. Completar Phase 4: User Story 2
5. **PARAR e VALIDAR**: rodar os passos 1-3 de `quickstart.md` (desenhar, publicar, executar um fluxo automático)
6. Demonstrar o MVP

### Incremental Delivery

1. Setup + Foundational → fundação pronta
2. US1 + US2 → MVP demonstrável (desenhar, publicar, executar automaticamente)
3. US3 → workflow com tarefa humana
4. US4 → workflow com integração REST real
5. US5 → decisão + mensageria (prova de conceito completa da seção 19)
6. US6 → operação e auditoria em produção
7. Cada incremento soma valor sem quebrar os anteriores

### Parallel Team Strategy

Com múltiplos desenvolvedores (seção 18 do planejamento.md):

- **Frontend**: designer, formulários, inbox de tarefas, visualização de execuções (T026-T031, T040, T049-T050, T058, T066, T073-T074)
- **Backend Core**: definições, versionamento, motor, máquina de estados, persistência, concorrência (T020-T025, T032-T039, T043-T048, T061-T065, T069-T072)
- **Integrações e Plataforma**: conectores, mensageria, segurança, observabilidade, infraestrutura (T007-T014, T051-T057, T064, T075-T077)

---

## Notes

- [P] = arquivos diferentes, sem dependência
- [Story] mapeia a tarefa a uma user story para rastreabilidade
- Cada user story deve ser completável e testável de forma independente
- Verificar que os testes falham antes de implementar
- Fazer commit após cada tarefa ou grupo lógico
- Parar em cada checkpoint para validar a story isoladamente
- Total: 80 tarefas (T001-T080) — 6 no Setup, 10 no Foundational, 63 distribuídas nas 6 user stories, 6 no Polish
- Acessibilidade (constitution II) é tratada desde T026-T028 (US1) e T050/T073, não só em T075 — ver `/speckit-analyze` (achado C1)
- SC-008 (escala) tem verificação dedicada em T080 — ver `/speckit-analyze` (achado H1)
