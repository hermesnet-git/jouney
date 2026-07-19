# Data Model: Plataforma de Workflows (MVP)

Fonte: entidades-chave de `spec.md`, detalhadas com os campos e tabelas já
definidos nas seções 4 e 7 do `planejamento.md`.

## Workflow Definition

Workflow em edição (rascunho); pode mudar livremente até ser publicado.

| Campo | Tipo | Notas |
|---|---|---|
| id | UUID | PK |
| workflow_key | string | identificador estável do workflow (ex.: `customer-onboarding`) |
| name | string | nome exibido |
| description | string | opcional |
| status | enum | `DRAFT` \| `PUBLISHED` (tem ao menos uma versão publicada) |
| created_by | string | autor |
| created_at / updated_at | timestamp | |

**Regras**: `workflow_key` é único. Um Workflow Definition não é executável
diretamente — só suas Workflow Versions publicadas são.

## Workflow Version

Fotografia imutável de uma Workflow Definition no momento da publicação
(spec FR-006).

| Campo | Tipo | Notas |
|---|---|---|
| id | UUID | PK |
| workflow_definition_id | UUID | FK → Workflow Definition |
| version_number | int | sequencial por definition, imutável após criado |
| definition_json | JSONB | grafo validado (nodes/edges), formato da seção 5 |
| published_by | string | |
| published_at | timestamp | |
| active | boolean | indica a versão-alvo padrão para novas execuções (Assumptions do spec) |

**Regras**: Nunca é alterada após criada (FR-006). Apenas uma versão por
`workflow_definition_id` pode estar `active = true` por vez.

## Workflow Instance (Execução)

Execução concreta de uma Workflow Version.

| Campo | Tipo | Notas |
|---|---|---|
| id | UUID | PK |
| workflow_version_id | UUID | FK → Workflow Version |
| business_key | string | opcional, correlação com o domínio de negócio |
| status | enum | `CREATED, RUNNING, WAITING, COMPLETED, FAILED, CANCELLED` (seção 6) |
| started_by | string | |
| started_at / completed_at | timestamp | |
| current_node_id | string | referência ao nó corrente no grafo |
| lock_version | long | optimistic locking (`@Version`, seção 12) |

**Transições de estado** (seção 6): `CREATED → RUNNING → (WAITING ⇄ RUNNING)* → COMPLETED | FAILED | CANCELLED`.
`WAITING` ocorre ao aguardar uma Human Task; um comando de retomada volta para
`RUNNING`. `CANCELLED` só é alcançável a partir de `RUNNING`/`WAITING` por ação
do Operador (US6).

## Step Instance (Node Instance)

Execução de uma etapa específica dentro de uma Workflow Instance.

| Campo | Tipo | Notas |
|---|---|---|
| id | UUID | PK |
| workflow_instance_id | UUID | FK → Workflow Instance |
| node_id | string | id do nó no grafo publicado |
| node_type | enum | `START, END, USER_TASK, REST_SERVICE_TASK, EXCLUSIVE_GATEWAY` (tipos do MVP, spec FR-002) |
| status | enum | `PENDING, RUNNING, WAITING, COMPLETED, FAILED, CANCELLED, SKIPPED` (seção 6) |
| attempt | int | tentativa atual (para retry, seção 12) |
| input_json / output_json | JSONB | dados de entrada/saída da etapa |
| error_json | JSONB | detalhe do erro, quando `FAILED` |
| started_at / completed_at | timestamp | |

**Regra de idempotência**: chamadas a sistemas externos usam
`workflow_instance_id` + `node_id` (node_instance) + `attempt` como chave de
idempotência (seção 12).

## Workflow Variable

Variáveis da execução, usadas por mapeamentos de entrada/saída e condições de
decisão.

| Campo | Tipo | Notas |
|---|---|---|
| workflow_instance_id | UUID | FK → Workflow Instance |
| variables_json | JSONB | chave/valor livre, seção 7 |
| updated_at | timestamp | |

## Human Task

Unidade de trabalho apresentada a um usuário/grupo (US3).

| Campo | Tipo | Notas |
|---|---|---|
| id | UUID | PK |
| workflow_instance_id | UUID | FK → Workflow Instance |
| node_instance_id | UUID | FK → Step Instance (a etapa `USER_TASK` correspondente) |
| name | string | |
| status | enum | `PENDING, CLAIMED, COMPLETED, CANCELLED` |
| assignee | string | nulo até ser assumida |
| candidate_group | string | grupo elegível, quando não há assignee direto |
| form_schema_json | JSONB | schema declarativo (seção 9: Text, Number, Date, Boolean, Select, Textarea) |
| form_data_json | JSONB | dados submetidos |
| created_at / claimed_at / completed_at | timestamp | |

**Regra de concorrência**: assumir (`claim`) é uma operação exclusiva — o
primeiro usuário do grupo a assumir consegue; os demais recebem indicação de
indisponibilidade (edge case do spec).

## Connector

Integração configurada, referenciada por etapas `REST_SERVICE_TASK`.

| Campo | Tipo | Notas |
|---|---|---|
| id | string | ex.: `customer-api-prod` |
| type | enum | `REST` no MVP (KAFKA/RABBITMQ preparados para depois, seção 10) |
| base_config_json | JSONB | endereço/operações; nunca contém a credencial em si |
| credential_ref | string | referência ao segredo no cofre externo (Vault/Secret Manager/K8s Secrets) |

## Audit Entry

Registro de uma ação para a trilha auditável (US6, FR-021).

| Campo | Tipo | Notas |
|---|---|---|
| id | UUID | PK |
| actor | string | quem realizou a ação |
| action | enum | `WORKFLOW_PUBLISHED, EXECUTION_STARTED, EXECUTION_COMPLETED, EXECUTION_CANCELLED, TASK_COMPLETED, EXECUTION_RETRIED, ...` |
| entity_type / entity_id | string / UUID | a que a ação se refere |
| occurred_at | timestamp | |
| detail_json | JSONB | payload adicional relevante à ação |

## Outbox Event

Suporte técnico ao Outbox Pattern (seção 12) — não é uma entidade de domínio
do spec, mas é necessária para US5 (publicação de evento) funcionar de forma
consistente com a transação de estado.

| Campo | Tipo | Notas |
|---|---|---|
| id | UUID | PK |
| aggregate_type / aggregate_id | string / UUID | normalmente `WORKFLOW_INSTANCE` |
| payload_json | JSONB | evento a publicar |
| status | enum | `PENDING, PUBLISHED, FAILED` |
| created_at / published_at | timestamp | |

## Relacionamentos

```text
Workflow Definition 1───N Workflow Version
Workflow Version    1───N Workflow Instance
Workflow Instance   1───N Step Instance
Workflow Instance   1───1 Workflow Variable
Step Instance       1───0..1 Human Task   (apenas para node_type = USER_TASK)
Step Instance       N───1 Connector       (apenas para node_type = REST_SERVICE_TASK)
Workflow Instance   1───N Audit Entry (indireto, via ações sobre a instância)
Workflow Instance   1───N Outbox Event
```
