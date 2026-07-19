# Research: Plataforma de Workflows (MVP)

Todas as decisões abaixo já estavam tomadas em `planejamento.md` (documento de
arquitetura pré-existente); este documento apenas as consolida no formato
Decision/Rationale/Alternatives exigido pelo Phase 0 do `/speckit-plan`. Não
restou nenhum `NEEDS CLARIFICATION` no Technical Context do `plan.md`.

## 1. Monólito modular (não microserviços)

- **Decision**: Backend único (Java/Spring Boot) dividido em 9 módulos de
  domínio (definition, publication, runtime, human-task, connector,
  expression, identity, audit, shared-kernel).
- **Rationale**: Seção 2 do planejamento.md — menor complexidade operacional
  para validar o MVP, com separação de módulos que permite extração futura
  para serviços independentes sem reescrever o domínio.
- **Alternatives considered**: Microserviços desde o início (rejeitado: custo
  operacional/infra prematuro antes de validar o modelo de domínio); adotar um
  motor BPM de terceiros pronto (Camunda/Flowable) (rejeitado: acopla a
  plataforma a um motor BPMN completo — a spec exclui explicitamente "BPMN
  completo" do escopo do MVP).

## 2. PostgreSQL com JSONB seletivo

- **Decision**: PostgreSQL único; `definition_json`, `variables_json` e os
  schemas de formulário em JSONB; status, chaves e campos operacionais
  frequentemente consultados permanecem em colunas relacionais.
- **Rationale**: Seção 7 — o grafo de um workflow varia demais em forma para
  um schema relacional fixo, mas estado de execução precisa de índices e
  consultas relacionais eficientes (filas de tarefas, listagem de instâncias).
- **Alternatives considered**: Banco de documentos puro (rejeitado: perde
  garantias transacionais para estado operacional/concorrência); tudo
  normalizado em tabelas relacionais (rejeitado: exigiria remodelar o schema a
  cada novo tipo de etapa).

## 3. Motor como máquina de estados persistida

- **Decision**: O motor não mantém a requisição HTTP aberta durante a
  execução; avança automaticamente até aguardar uma tarefa humana, finalizar
  ou falhar, e retoma via um novo comando.
- **Rationale**: Seção 6 — necessário porque tarefas humanas podem levar horas
  ou dias; manter uma conexão aberta nesse intervalo é inviável.
- **Alternatives considered**: Execução síncrona bloqueante (rejeitada: só
  funciona apontando exclusivamente para etapas automáticas); fila de
  orquestração externa dedicada já no MVP (adiada: o comando de retomada e o
  Outbox Pattern já cobrem o essencial nesta fase, sem adicionar um
  componente de infraestrutura novo).

## 4. Definição de workflow e formulários declarativos (sem scripts arbitrários)

- **Decision**: JSON declarativo tanto para o grafo do workflow (nodes/edges,
  seção 5) quanto para formulários de tarefa humana (schema com tipos Text,
  Number, Date, Boolean, Select, Textarea, seção 9); o backend revalida tudo
  na publicação/submissão, nunca confiando cegamente no JSON do designer.
- **Rationale**: Mantém o designer seguro contra execução de código arbitrário
  e o schema totalmente serializável, versionável e auditável.
- **Alternatives considered**: DSL de expressão completa tipo linguagem de
  programação (rejeitada nesta fase: amplia a superfície de risco de segurança
  e a complexidade do validador sem necessidade demonstrada no MVP).

## 5. Conectores desacoplados, credenciais fora do JSON do workflow

- **Decision**: Interface `ConnectorExecutor` (supports/execute) com
  implementações por tipo (REST, Kafka, RabbitMQ); o workflow referencia um
  `connectorId`, e a credencial real fica em um cofre externo (Secret
  Manager/Vault/Kubernetes Secrets).
- **Rationale**: Seções 10 e 13 — permite adicionar tipos de conector sem
  alterar o motor, e evita que segredos apareçam em definições de workflow
  versionadas e auditadas (que ficam armazenadas indefinidamente).
- **Alternatives considered**: Credencial inline na configuração da etapa
  (rejeitada: vaza para histórico de versão e trilha de auditoria).

## 6. Concorrência: optimistic locking + idempotência + Outbox Pattern

- **Decision**: `@Version`/`lockVersion` na instância; chave de idempotência
  composta por `executionId`/`workflowInstanceId`/`nodeInstanceId`/`attempt`
  enviada a sistemas externos; Outbox Pattern para mensageria (evento gravado
  na mesma transação do estado, publicado depois por um processo assíncrono).
- **Rationale**: Seção 12 — evita processamento duplicado em reprocessamento/
  retry e garante que a publicação de um evento nunca fique inconsistente com
  o estado persistido da instância.
- **Alternatives considered**: Locking pessimista (rejeitado: reduz throughput
  sem necessidade demonstrada na escala alvo do MVP — SC-008); publicar a
  mensagem diretamente no mesmo passo da chamada externa, sem outbox
  (rejeitado: risco de publicar um evento e depois falhar ao persistir o
  estado, ou vice-versa).

## 7. Estratégia de testes herdada do planejamento.md

- **Decision**: Backend — JUnit 5 + PostgreSQL local real (sem containers) para
  integração, unitários para transições/expressões/estado/retry, testes de
  arquitetura para impedir dependência indevida entre os 9 módulos. Frontend —
  testes de componente para nós customizados/painel de propriedades/
  serialização/validação de grafo, mais testes end-to-end do fluxo
  designer→publicação→execução. Os testes de integração que precisam de banco
  são marcados com `@Tag("requires-postgres")` e rodam separados via Maven
  Failsafe (`mvn verify`); os demais rodam em `mvn test` (Surefire), incluindo
  vários `*IntegrationTest` que não tocam banco (usam mocks ou um `HttpServer`
  local) — a separação é pela tag, não pelo nome do arquivo.
- **Rationale**: Seção 15 do planejamento.md já define essa estratégia em
  detalhe; mantê-la evita divergência entre teste com mock e comportamento
  real do banco/motor.
- **Alternatives considered**: Mockar o banco em todos os testes (rejeitado no
  próprio doc-fonte). Testcontainers/Docker para provisionar o Postgres nos
  testes (tentado primeiro, revertido — ver nota de ambiente abaixo). Rodar
  tudo em `mvn test` sem separação (rejeitado: quebraria CI e qualquer
  desenvolvedor sem Postgres local rodando).
- **Nota de gate**: a constitution v2.0.0 deste repositório não tem mais um
  princípio de Testing Standards (removido em sessão anterior). Esta decisão é
  registrada como escolha técnica herdada do planejamento.md, não como
  exigência automática da constitution — sua aplicação real depende das
  tarefas de teste que `/speckit-tasks` gerar explicitamente.
- **Nota de ambiente descoberta durante `/speckit-implement`**: o JDK 24
  instalado neste ambiente de desenvolvimento não é compatível com o
  ByteBuddy usado pelo Mockito para mockar classes concretas (`mock()` de uma
  `@Service` como `EngineDispatcher`/`AuditService` falha; mockar interfaces
  como os `*Repository` do Spring Data funciona normalmente). Os testes deste
  repositório evitam mockar classes concretas — quando um teste precisaria
  disso, ele instancia a classe real com suas dependências de interface
  mockadas. Se este projeto migrar para um JDK 17/21 LTS estável, essa
  restrição deixa de existir.
- **Nota de ambiente — Docker removido**: Docker foi tentado inicialmente
  (Testcontainers) mas o ambiente de desenvolvimento não tinha WSL2
  configurado e o daemon nunca ficou disponível de forma confiável nesta
  sessão. O usuário optou por instalar PostgreSQL localmente em vez de
  destravar o Docker. Toda a configuração de Docker foi removida do
  repositório (`docker-compose.yml`, dependências `org.testcontainers.*` e o
  BOM correspondente no `pom.xml`); os 3 testes que precisam de banco foram
  reescritos para usar o `DataSource` configurado em `application.yml`
  (`localhost:5432/workflow_platform`) em vez de um container efêmero, e
  marcados `@Transactional` para desfazer os dados ao final de cada teste
  (necessário porque o banco agora é persistente entre execuções, diferente
  de um container Testcontainers descartado a cada rodada).
- **Bugs reais encontrados só ao rodar contra Postgres de verdade** (nunca
  detectados antes porque os testes de integração nunca tinham rodado neste
  ambiente): (1) os testes chamavam `WorkflowDefinition.updateDraft(...)`
  diretamente na entidade após `repository.save(...)`, o que não persiste —
  corrigido para usar `WorkflowDefinitionService.updateDraft(...)`, o mesmo
  caminho que `WorkflowController` usa de verdade; (2) `GraphValidator`
  exigia o campo `formKey` numa etapa `USER_TASK`, mas `UserTaskNodeExecutor`
  (e o designer no frontend) usam `formSchema` embutido — nenhum workflow com
  tarefa humana conseguia ser publicado. Corrigido nos dois lados
  (`GraphValidator.java` e `PropertiesPanel.tsx`), padronizando em
  `formSchema`.
