# API Contracts: Plataforma de Workflows (MVP)

REST/JSON, autenticação via OpenID Connect (Bearer token), autorização por
papel (spec FR-022). Endpoints e agrupamento herdados da seção 11 do
planejamento.md; contratos completos (schemas de request/response) são
detalhados em `tasks.md`/implementação, não duplicados aqui.

## Workflows (workflow-definition / workflow-publication)

| Método | Path | Propósito | Papel mínimo | Requisito |
|---|---|---|---|---|
| POST | `/api/workflows` | Cria um novo Workflow Definition (rascunho) | WORKFLOW_DESIGNER | FR-001, FR-005 |
| GET | `/api/workflows` | Lista workflows (rascunhos e publicados) | WORKFLOW_DESIGNER, WORKFLOW_OPERATOR | FR-019 |
| GET | `/api/workflows/{id}` | Detalha um Workflow Definition | WORKFLOW_DESIGNER, WORKFLOW_OPERATOR | — |
| PUT | `/api/workflows/{id}` | Atualiza o rascunho (grafo, config) | WORKFLOW_DESIGNER | FR-001, FR-003, FR-005 |
| POST | `/api/workflows/{id}/validate` | Valida o rascunho sem publicar | WORKFLOW_DESIGNER | FR-004 |
| POST | `/api/workflows/{id}/publish` | Publica uma nova Workflow Version imutável | WORKFLOW_PUBLISHER | FR-004, FR-006 |
| GET | `/api/workflows/{id}/versions` | Lista as versões publicadas | WORKFLOW_DESIGNER, WORKFLOW_OPERATOR | — |

`POST .../publish` retorna 422 com a lista completa de problemas quando a
validação falha (FR-004) — nunca publica parcialmente.

## Execuções (workflow-runtime)

| Método | Path | Propósito | Papel mínimo | Requisito |
|---|---|---|---|---|
| POST | `/api/workflow-versions/{versionId}/instances` | Inicia uma nova execução | WORKFLOW_OPERATOR | FR-007 |
| GET | `/api/instances/{instanceId}` | Status atual da execução | WORKFLOW_OPERATOR | FR-019 |
| GET | `/api/instances/{instanceId}/history` | Histórico de Step Instances (início/fim/resultado) | WORKFLOW_OPERATOR | FR-009 |
| POST | `/api/instances/{instanceId}/cancel` | Cancela uma execução em andamento | WORKFLOW_OPERATOR | FR-020, US6 |
| POST | `/api/instances/{instanceId}/retry` | Reprocessa a partir da etapa falha | WORKFLOW_OPERATOR | FR-020, US6 |

Estes endpoints disparam o motor de forma assíncrona (o motor avança até
`WAITING`/estado final e retorna — ver `research.md` #3); não bloqueiam a
requisição HTTP até a execução terminar.

## Tarefas humanas (human-task)

| Método | Path | Propósito | Papel mínimo | Requisito |
|---|---|---|---|---|
| GET | `/api/tasks` | Lista tarefas pendentes do usuário/grupo autenticado | TASK_USER | FR-010 |
| GET | `/api/tasks/{taskId}` | Detalha uma tarefa (inclui `form_schema_json`) | TASK_USER | FR-010 |
| POST | `/api/tasks/{taskId}/claim` | Assume a tarefa (exclusivo) | TASK_USER | FR-011, edge case de concorrência |
| POST | `/api/tasks/{taskId}/complete` | Submete `form_data_json` e conclui | TASK_USER | FR-011, FR-012, FR-013 |

`claim` retorna 409 quando a tarefa já foi assumida por outro usuário do
mesmo grupo. `complete` retorna 422 com os campos obrigatórios pendentes
quando a validação de formulário falha (FR-012).

## Conectores (connector)

| Método | Path | Propósito | Papel mínimo | Requisito |
|---|---|---|---|---|
| POST | `/api/connectors` | Cadastra um conector (config, sem credencial inline) | PLATFORM_ADMIN | FR-014, seção 10/13 |
| GET | `/api/connectors` | Lista conectores | WORKFLOW_DESIGNER, PLATFORM_ADMIN | — |
| GET | `/api/connectors/{id}` | Detalha um conector | WORKFLOW_DESIGNER, PLATFORM_ADMIN | — |
| PUT | `/api/connectors/{id}` | Atualiza configuração do conector | PLATFORM_ADMIN | — |
| POST | `/api/connectors/{id}/test` | Testa a conectividade/credencial referenciada | PLATFORM_ADMIN | seção 10 |

## Auditoria (audit)

| Método | Path | Propósito | Papel mínimo | Requisito |
|---|---|---|---|---|
| GET | `/api/audit` | Consulta a trilha de auditoria (filtrável por entidade/ator/período) | WORKFLOW_OPERATOR, PLATFORM_ADMIN | FR-021, US6 |

## Erros

Todas as respostas de erro seguem um formato único (`code`, `message`,
`details[]`) para satisfazer a constitution (princípio II — mensagem de erro
deve dizer o que aconteceu e o que fazer a seguir), reaproveitado por todos os
grupos de endpoint acima.
