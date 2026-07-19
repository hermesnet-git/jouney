# Plano inicial para construção da plataforma de workflows

A recomendação é iniciar com um **MVP vertical**, capaz de criar, publicar e executar um workflow simples de ponta a ponta.

O primeiro objetivo não deve ser construir um BPM completo. Deve ser entregar um fluxo funcional como:

> Início → Formulário → Chamada REST → Aprovação humana → Fim

Esse fluxo permitirá validar as decisões arquiteturais mais importantes da plataforma.

---

# 1. Definir claramente o escopo do MVP

## Funcionalidades incluídas

### Aplicação Web — Workflow Designer

O usuário poderá:

- Criar um workflow.
- Adicionar nós em uma área visual.
- Conectar os nós.
- Configurar cada nó.
- Validar o workflow.
- Salvar como rascunho.
- Publicar uma versão.
- Iniciar uma execução.
- Visualizar o status da execução.

### Motor de Workflow

O motor deverá:

- Interpretar a definição publicada.
- Criar uma instância de execução.
- Controlar o estado do workflow.
- Executar tarefas automáticas.
- Criar tarefas humanas.
- Aguardar respostas externas.
- Registrar o histórico da execução.
- Continuar o fluxo após cada tarefa.
- Tratar erros básicos e tentativas de reprocessamento.

## Tipos de nós do primeiro MVP

1. **Start**
2. **End**
3. **User Task**
4. **REST Service Task**
5. **Message Publish Task**
6. **Exclusive Gateway**
7. **Timer ou Wait Task**, opcional na primeira versão

Evitar inicialmente subworkflows, execução paralela, compensações, transações distribuídas, alteração de workflow em execução, BPMN completo, scripts arbitrários e marketplace de conectores.

---

# 2. Arquitetura inicial da plataforma

```text
┌─────────────────────────────────────────────┐
│             Workflow Designer               │
│ React + React Flow                          │
│ - Modelagem visual, configuração, validação │
│ - Publicação e acompanhamento de execuções  │
└───────────────────┬─────────────────────────┘
                    │ REST API
                    ▼
┌─────────────────────────────────────────────┐
│         Workflow Platform Backend           │
│ Java + Spring Boot                          │
│ - Definições, versionamento e execução      │
│ - Tarefas humanas, conectores e auditoria   │
└──────────────┬─────────────────┬────────────┘
               │                 │
               ▼                 ▼
       ┌───────────────┐  ┌──────────────────┐
       │ PostgreSQL    │  │ Sistemas externos│
       │               │  │ REST / Mensageria│
       └───────────────┘  └──────────────────┘
```

Para o início, o backend deve ser um **monólito modular**: menor complexidade operacional, com separações que permitam futura evolução para serviços independentes.

---

# 3. Módulos do backend

```text
workflow-platform
├── workflow-definition
├── workflow-publication
├── workflow-runtime
├── human-task
├── connector
├── expression
├── identity
├── audit
└── shared-kernel
```

## 3.1 Workflow Definition

Responsável por criar workflows, alterar rascunhos, salvar o grafo, validar nós e conexões, manter metadados e consultar definições.

## 3.2 Workflow Publication

Responsável por publicar versões imutáveis, gerar o número da versão, validar antes da publicação, impedir alterações publicadas e identificar a versão ativa.

## 3.3 Workflow Runtime

Núcleo do motor: cria instâncias, determina o próximo nó, executa tarefas automáticas, aguarda tarefas humanas, avalia condições, atualiza estados, trata erros e tentativas e finaliza ou cancela instâncias.

## 3.4 Human Task

Cria e consulta tarefas de usuários ou grupos; permite assumir, preencher e concluir formulários; registra o resultado e retoma a execução.

## 3.5 Connector

Registra integrações, executa REST e mensageria, resolve credenciais, normaliza respostas e erros. Posteriormente pode consumir mensagens.

## 3.6 Expression

Avalia condições de gateways, mapeamentos de entrada e saída e variáveis do workflow. No início, usar linguagem declarativa limitada, sem código arbitrário:

```text
customer.age >= 18
order.amount > 1000 && customer.type == "PREMIUM"
```

## 3.7 Audit

Registra autoria e alterações de definições, publicações, mudanças de estado, execuções dos nós, entradas e saídas de tarefas, erros e reprocessamentos.

---

# 4. Modelo conceitual principal

## Workflow Definition

Workflow em edição; pode mudar enquanto estiver em rascunho.

## Workflow Version

Fotografia imutável de uma definição publicada.

```text
Onboarding de Cliente
- Versão 1
- Versão 2
- Versão 3
```

## Workflow Instance

Execução concreta de uma versão.

```text
Workflow: Onboarding de Cliente
Versão: 3
Instância: 58c472...
Cliente: 12345
Status: WAITING_USER_TASK
```

## Node Instance

Execução de um nó específico dentro da instância.

```text
Nó: Validar CPF
Tipo: REST_SERVICE_TASK
Status: COMPLETED
Tentativas: 1
Início: 14:05
Fim: 14:05
```

Essa separação é essencial para auditoria, versionamento e rastreabilidade.

---

# 5. Formato da definição do workflow

O React Flow trabalha com coleções de nós e arestas; essa estrutura pode ser convertida para uma definição JSON persistida pelo backend.

```json
{
  "workflowKey": "customer-onboarding",
  "name": "Onboarding de Cliente",
  "startNodeId": "start-1",
  "nodes": [
    { "id": "start-1", "type": "START", "name": "Início", "configuration": {} },
    {
      "id": "form-1", "type": "USER_TASK", "name": "Informar dados pessoais",
      "configuration": { "formKey": "personal-data", "assignment": { "type": "INITIATOR" } }
    },
    {
      "id": "service-1", "type": "REST_SERVICE_TASK", "name": "Validar cliente",
      "configuration": {
        "connectorId": "customer-api", "operation": "validateCustomer",
        "inputMapping": { "cpf": "${workflow.customer.cpf}" },
        "outputMapping": { "validationResult": "$.result" }
      }
    },
    { "id": "gateway-1", "type": "EXCLUSIVE_GATEWAY", "name": "Cliente válido?", "configuration": {} },
    { "id": "end-1", "type": "END", "name": "Fim", "configuration": {} }
  ],
  "edges": [
    { "id": "edge-1", "source": "start-1", "target": "form-1" },
    { "id": "edge-2", "source": "form-1", "target": "service-1" },
    { "id": "edge-3", "source": "service-1", "target": "gateway-1" },
    { "id": "edge-4", "source": "gateway-1", "target": "end-1", "condition": "${validationResult == 'APPROVED'}" }
  ]
}
```

O JSON do designer não deve ser executado cegamente: na publicação, o backend deve validá-lo e convertê-lo para um modelo interno confiável.

---

# 6. Modelo de execução do motor

Usar uma **máquina de estados persistida**.

1. Usuário inicia uma instância.
2. Motor localiza o nó inicial.
3. Cria instância do nó.
4. Executa o comportamento correspondente.
5. Persiste resultado.
6. Determina próxima transição.
7. Executa próximo nó ou coloca instância em espera.

## Estados da instância

```text
CREATED
RUNNING
WAITING
COMPLETED
FAILED
CANCELLED
```

## Estados de um nó

```text
PENDING
RUNNING
WAITING
COMPLETED
FAILED
CANCELLED
SKIPPED
```

Não manter transação HTTP aberta durante toda a execução. O motor avança até aguardar uma tarefa, finalizar ou encontrar erro. Ao concluir uma tarefa humana, um novo comando retoma o processamento.

---

# 7. Persistência no PostgreSQL

Tabelas iniciais:

```text
workflow_definition
workflow_version
workflow_instance
node_instance
workflow_variable
human_task
connector_definition
execution_event
outbox_event
```

Estrutura aproximada:

```text
workflow_definition: id, workflow_key, name, description, status, created_by, created_at, updated_at
workflow_version: id, workflow_definition_id, version_number, definition_json, published_by, published_at
workflow_instance: id, workflow_version_id, business_key, status, started_by, started_at, completed_at, current_node_id, lock_version
node_instance: id, workflow_instance_id, node_id, node_type, status, attempt, input_json, output_json, error_json, started_at, completed_at
human_task: id, workflow_instance_id, node_instance_id, name, status, assignee, candidate_group, form_schema_json, form_data_json, created_at, claimed_at, completed_at
```

No MVP, `workflow_variable` pode conter `workflow_instance_id`, `variables_json` e `updated_at`. JSONB atende bem definições e variáveis, desde que estados, relacionamentos e campos operacionais principais permaneçam relacionais.

---

# 8. Aplicação React com React Flow

```text
src
├── app
├── workflow-designer
│   ├── canvas
│   ├── nodes
│   ├── edges
│   ├── palette
│   ├── properties-panel
│   ├── validation
│   └── serialization
├── workflow-management
├── workflow-runtime
├── task-inbox
├── connectors
├── shared
└── api
```

O designer terá Palette (início, fim, usuário, REST, mensagem e gateway), Canvas para inserir/conectar/mover/excluir nós, Properties Panel para configurar o nó e Validation Panel.

Exemplo de configuração REST:

```text
Nome: Consultar cliente
Conector: Customer API
Operação: GET /customers/{id}
Timeout: 5 segundos
Tentativas: 3
Mapeamento de entrada e de saída
```

Validações: workflow sem início ou com mais de um início, nó sem conexão, gateway sem condição, REST sem conector, User Task sem formulário, nó inalcançável e fluxo sem fim. O Execution Viewer pode reutilizar React Flow para mostrar nós concluídos, atual, com erro e caminho percorrido.

---

# 9. Formulários das tarefas humanas

Formulários devem ser declarativos:

```json
{
  "title": "Dados pessoais",
  "fields": [
    { "key": "name", "label": "Nome", "type": "text", "required": true },
    { "key": "cpf", "label": "CPF", "type": "text", "required": true },
    { "key": "birthDate", "label": "Data de nascimento", "type": "date", "required": true }
  ]
}
```

Tipos iniciais: Text, Number, Date, Boolean, Select e Textarea. O frontend renderiza dinamicamente; o backend valida novamente os dados submetidos.

---

# 10. Arquitetura de conectores

Conectores são separados da definição. Um conector representa uma integração configurada; uma operação, uma ação disponível nela.

```java
public interface ConnectorExecutor {
    boolean supports(ConnectorType type);
    ConnectorResult execute(
        ConnectorDefinition connector,
        ConnectorOperation operation,
        Map<String, Object> input,
        ExecutionContext context
    );
}
```

Implementações: `RestConnectorExecutor`, `KafkaPublishConnectorExecutor` e `RabbitMqPublishConnectorExecutor`; depois, consumidores Kafka, banco de dados, SFTP, e-mail e webhook.

Credenciais não ficam no JSON. O workflow referencia, por exemplo, `customer-api-prod`; a configuração protegida fica em Secret Manager, Vault, Kubernetes Secrets ou serviço corporativo de credenciais.

---

# 11. APIs iniciais

```text
POST   /api/workflows
GET    /api/workflows
GET    /api/workflows/{id}
PUT    /api/workflows/{id}
POST   /api/workflows/{id}/validate
POST   /api/workflows/{id}/publish
GET    /api/workflows/{id}/versions

POST   /api/workflow-versions/{versionId}/instances
GET    /api/instances/{instanceId}
GET    /api/instances/{instanceId}/history
POST   /api/instances/{instanceId}/cancel
POST   /api/instances/{instanceId}/retry

GET    /api/tasks
GET    /api/tasks/{taskId}
POST   /api/tasks/{taskId}/claim
POST   /api/tasks/{taskId}/complete

POST   /api/connectors
GET    /api/connectors
GET    /api/connectors/{id}
PUT    /api/connectors/{id}
POST   /api/connectors/{id}/test
```

---

# 12. Concorrência e confiabilidade

Usar optimistic locking na instância:

```java
@Version
private Long lockVersion;
```

Serviços devem ser idempotentes, usando `executionId`, `workflowInstanceId`, `nodeInstanceId` e `attempt` como chave enviada ao sistema externo. Para mensagens, aplicar Outbox Pattern: na mesma transação, atualizar a instância e gravar evento; um publicador assíncrono o envia depois.

Cada nó de serviço permite retry:

```json
{ "maxAttempts": 3, "initialDelay": "PT2S", "backoffMultiplier": 2 }
```

Distinguir erro temporário, de negócio, de configuração e não recuperável.

---

# 13. Segurança

- Autenticação OpenID Connect e autorização por papéis.
- Papéis: `WORKFLOW_DESIGNER`, `WORKFLOW_PUBLISHER`, `WORKFLOW_OPERATOR`, `TASK_USER`, `PLATFORM_ADMIN`.
- Proteção de credenciais, mascaramento de informações sensíveis e auditoria.
- Validação de URLs, proteção contra SSRF, limite de payload e timeout obrigatório em chamadas externas.

---

# 14. Observabilidade

Cada execução carrega `correlationId`, `workflowInstanceId`, `nodeInstanceId` e `businessKey`. Métricas: instâncias iniciadas/concluídas/com erro, tempos médios por workflow e nó, tarefas pendentes, erros por conector, retentativas e tempo de resposta de integrações. Logs estruturados preferencialmente em JSON.

---

# 15. Estratégia de testes

Backend: testes unitários de transições, expressões, variáveis, estados, retry e validação; integração com PostgreSQL real via containers, REST mockado, mensageria, concorrência e retomada; testes de arquitetura para impedir dependências indevidas.

Frontend: testes dos nós customizados, painel de propriedades, serialização, validação de grafo, criação de conexões e testes end-to-end.

---

# 16. Fases de construção

## Fase 0 — Descoberta e decisões fundamentais

Entregáveis: visão do produto, glossário, casos de uso, escopo MVP, nós iniciais, estados, JSON e decisões documentadas. Registrar: versão imutável após publicação, motor persistido, monólito modular, formulários declarativos, conectores desacoplados, retomada e JSONB.

## Fase 1 — Skeleton técnico

Frontend React/React Flow com layout, canvas, palette, painel e Start/End. Backend Spring Boot modular, PostgreSQL, migrações, erros, OpenAPI, autenticação e CI. Resultado: web, backend e banco comunicando-se.

## Fase 2 — Designer básico

Criar workflow, nós, conexões, propriedades, salvar/carregar rascunho, validar e publicar. Resultado: usuário desenha e publica, ainda sem execução.

## Fase 3 — Motor mínimo

Criar instâncias, Start/End, navegação sequencial, estados, histórico, variáveis e retomada. Resultado: fluxo automático simples executado e auditado.

## Fase 4 — Tarefa humana e formulários

Schema, renderizador, inbox, claim, complete e integração com runtime. Resultado: usuário conclui uma tarefa por formulário e o fluxo retoma.

## Fase 5 — Conector REST

Cadastro, operações, executor, autenticação, mapeamentos, timeout, retry, teste e erros. Resultado: workflow chama integração REST real.

## Fase 6 — Decisões e mensageria

Gateway exclusivo, condições, publicação, outbox, Kafka ou RabbitMQ e caminho executado. Resultado: workflow toma decisões e publica eventos.

## Fase 7 — Operação e endurecimento

Painel de instâncias, retry, cancelamento, auditoria, métricas, tracing, acesso, limites, carga e recuperação.

---

# 17. Backlog técnico inicial

1. **Fundação:** repositórios, branches, pipelines, banco, migrações, autenticação, OpenAPI, logs e correlação.
2. **Modelagem:** definição/versão, schema JSON, validador, publicação e histórico.
3. **Designer:** canvas, palette, nós, conexões, propriedades, validação e salvamento.
4. **Runtime:** instância, nó, dispatcher, execução sequencial, estado, retomada e eventos.
5. **Human Task:** schema, renderizador, tarefa, inbox, claim, complete e runtime.
6. **REST Connector:** cadastro, operações, executor, autenticação, mapeamento, timeout/retry e teste.

---

# 18. Organização recomendada da equipe

- **Frontend:** designer, React Flow, formulários, caixa de tarefas e visualização de execuções.
- **Backend Core:** definições, versionamento, runtime, máquina de estados, persistência e concorrência.
- **Integrações e Plataforma:** conectores, mensageria, segurança, observabilidade, infraestrutura e pipeline.

Mesmo com pessoas acumulando papéis, a separação de responsabilidades deve permanecer clara.

---

# 19. Primeira prova de conceito

```text
[Início]
    ↓
[Preencher dados do cliente]
    ↓
[Consultar API de validação]
    ↓
[Cliente aprovado?]
   ↙               ↘
[Revisão manual]   [Publicar mensagem]
   ↓                  ↓
[Fim recusado]     [Fim aprovado]
```

Valida modelagem visual, persistência, versionamento, formulário dinâmico, tarefa humana, REST, gateway, variáveis, mensageria, histórico e visualização.

---

# 20. Primeiros passos concretos

1. Definir JSON canônico.
2. Definir estados e transições.
3. Criar modelo relacional inicial.
4. Criar Spring Boot modular.
5. Criar React com React Flow.
6. Implementar Start, End e User Task.
7. Salvar e publicar uma definição.
8. Criar uma instância.
9. Executar até a tarefa humana.
10. Concluir e retomar.
11. Adicionar REST.
12. Adicionar gateway e mensageria.

A primeira entrega deve demonstrar um fluxo pequeno do designer à conclusão; a plataforma evolui incrementalmente, sem antecipar toda a complexidade de um BPM corporativo.
