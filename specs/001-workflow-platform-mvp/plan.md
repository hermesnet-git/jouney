# Implementation Plan: Plataforma de Workflows (MVP)

**Branch**: `001-workflow-platform-mvp` | **Date**: 2026-07-19 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-workflow-platform-mvp/spec.md`

## Summary

Entregar um MVP vertical de plataforma de workflows: um Designer desenha, valida
e publica um workflow visualmente (React + React Flow), e um Operador inicia e
acompanha execuções desse workflow até a conclusão, incluindo tarefas humanas
com formulário, chamada a serviço REST externo e uma etapa de decisão que pode
publicar um evento. Abordagem técnica: backend Java/Spring Boot como monólito
modular com um motor de execução implementado como máquina de estados
persistida (sem manter conexão HTTP aberta durante a execução), PostgreSQL como
armazenamento único (JSONB para a definição do workflow e variáveis, colunas
relacionais para estado operacional), conectores desacoplados via interface,
publicação de eventos via Outbox Pattern. Decisões e tecnologias já definidas
em `planejamento.md` (seções 2-14 do documento-fonte); esta feature ainda não
tem nenhum código-fonte no repositório — toda a estrutura abaixo é criada do
zero.

## Technical Context

**Language/Version**: Java 17 (backend); TypeScript + React 18 (frontend)

**Primary Dependencies**: Spring Boot (web, data JPA, security/OAuth2 resource
server), PostgreSQL driver; React Flow (canvas de designer/visualização de
execução)

**Storage**: PostgreSQL — JSONB para `workflow_version.definition_json`,
`workflow_variable.variables_json`, `human_task.form_schema_json`/
`form_data_json`; colunas relacionais para status, chaves e campos consultados
com frequência (ver `data-model.md`)

**Testing**: JUnit 5 + Testcontainers (PostgreSQL real) para unitários/
integração de backend, testes de arquitetura para impedir dependências
indevidas entre módulos; Jest/Testing Library para componentes de designer
(nós, painel de propriedades, serialização) e testes end-to-end do fluxo
designer→publicação→execução no frontend (decisão herdada da seção 15 do
planejamento.md — ver nota de gate em Constitution Check)

**Target Platform**: Backend como serviço web em container Linux; frontend
como aplicação web servida ao navegador

**Project Type**: Web application (frontend + backend)

**Performance Goals**:
- Endpoints de CRUD/consulta (workflows, instâncias, tarefas) respondem em
  <300ms p95 sob a carga alvo (SC-008).
- Uma etapa automática (sem chamada externa) é processada pelo motor em
  <2s do fim da etapa anterior até o início da próxima.
- Listagem de instâncias/inbox de tarefas retorna em <1s para até algumas
  centenas de itens.

**Constraints**: Timeout obrigatório em toda chamada externa (padrão 5s,
configurável por etapa); sem sub-workflows, execução paralela, compensações/
transações distribuídas ou scripts arbitrários no designer (fora de escopo,
ver spec); credenciais de conector nunca residem no JSON do workflow.

**Scale/Scope**: Algumas dezenas de execuções simultâneas (piloto interno —
SC-008); 5 tipos de etapa no MVP (Start, End, User Task, REST Service Task,
Exclusive Gateway) mais publicação de evento em um caminho de decisão; 9
módulos de backend (ver Project Structure).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Avaliação contra `.specify/memory/constitution.md` v2.0.0:

- **I. Code Quality** — PASS. O monólito modular (9 módulos com responsabilidade
  única cada — ver Project Structure) já impõe a separação exigida pelo
  princípio; revisão de PR e lint limpo (Checkstyle/Spotless no backend,
  ESLint/Prettier no frontend) são pré-requisito de merge, não uma escolha
  desta feature.
- **II. User Experience Consistency** — PASS. O designer visual (React Flow) e
  o Execution Viewer devem reutilizar os mesmos componentes de nó/aresta e
  convenções de erro (spec FR-004, FR-012). Risco identificado: interfaces
  baseadas em canvas são historicamente fracas em acessibilidade por teclado —
  como o princípio exige que acessibilidade "MUST NOT be deferred as polish",
  isso é tarefa explícita já nas tarefas de UI de US1 (T026-T028) em
  `tasks.md`, com uma auditoria final em Polish (T075), não o inverso.
- **III. Performance Requirements** — PASS. Os budgets estão definidos acima em
  Technical Context (não foram assumidos como seguros sem medir); nenhuma
  dependência nova em hot path foi introduzida sem justificativa (o motor
  evita conexão HTTP longa exatamente para não criar um gargalo de threads/
  conexões — seção 6 do planejamento.md).

Nenhuma violação identificada. `Complexity Tracking` permanece vazio.

**Nota (não é gate)**: a constitution atual não tem mais um princípio de
Testing Standards (removido em sessão anterior a pedido do usuário). A
estratégia de testes acima (herdada da seção 15 do planejamento.md) é mantida
como decisão técnica registrada em `research.md`, mas não é imposta
automaticamente por este gate — sua cobertura real depende das tarefas
explícitas geradas em `/speckit-tasks`.

**Re-check pós-Phase 1**: `data-model.md` e `contracts/api.md` não introduzem
nenhum módulo, dependência ou padrão fora do que já foi avaliado acima — os 9
módulos, os grupos de endpoint e as entidades mapeiam 1:1 para a Structure
Decision. Gates I-III permanecem PASS.

## Project Structure

### Documentation (this feature)

```text
specs/001-workflow-platform-mvp/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
backend/
├── src/main/java/com/jouney/workflow/
│   ├── definition/         # workflow-definition: CRUD de rascunho, grafo, metadados
│   ├── publication/        # workflow-publication: versionamento imutável, validação de publicação
│   ├── runtime/             # workflow-runtime: motor, instâncias, transições, retry
│   ├── humantask/           # human-task: tarefas, inbox, claim/complete, formulários
│   ├── connector/           # connector: ConnectorExecutor + implementações (REST, Kafka, RabbitMQ)
│   ├── expression/          # expression: avaliação de condições/mapeamentos
│   ├── identity/            # identity: papéis, autenticação (OpenID Connect)
│   ├── audit/               # audit: trilha de auditoria
│   └── shared/               # shared-kernel: tipos e utilitários comuns entre módulos
└── src/test/java/com/jouney/workflow/
    ├── unit/                 # transições, expressões, validação, retry
    ├── integration/          # Testcontainers (Postgres real), REST mockado, retomada
    └── architecture/         # testes de dependência entre módulos

frontend/
├── src/
│   ├── app/                  # shell, rotas, layout
│   ├── workflow-designer/
│   │   ├── canvas/
│   │   ├── nodes/
│   │   ├── edges/
│   │   ├── palette/
│   │   ├── properties-panel/
│   │   ├── validation/
│   │   └── serialization/
│   ├── workflow-management/  # listagem/rascunhos/publicação
│   ├── workflow-runtime/     # execution viewer
│   ├── task-inbox/
│   ├── connectors/           # cadastro/teste de conectores
│   ├── shared/
│   └── api/                  # client HTTP gerado a partir de contracts/
└── tests/                    # componentes de nó, painel de propriedades, serialização, E2E
```

**Structure Decision**: Web application com dois projetos de topo, `backend/`
(monólito modular Java/Spring Boot, um pacote por módulo do domínio, seguindo
exatamente os 9 módulos definidos na seção 3 do planejamento.md) e `frontend/`
(React + React Flow, árvore `src/` da seção 8 do planejamento.md). Nenhum dos
dois existe ainda no repositório — são criados a partir da Fase de Setup em
`tasks.md`.

## Complexity Tracking

> Nenhuma violação de constitution identificada nesta feature — tabela
> intencionalmente vazia.
