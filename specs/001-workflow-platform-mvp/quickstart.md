# Quickstart: Validação ponta a ponta do MVP

Roteiro para provar que o fluxo vertical completo funciona, cobrindo US1-US5
(US6/operação é validada à parte, seção "Validação de operação" abaixo). Este
é o mesmo fluxo descrito na seção 19 do planejamento.md:

```text
[Início]
    ↓
[Preencher dados do cliente]      (User Task, US3)
    ↓
[Consultar API de validação]      (REST Service Task, US4)
    ↓
[Cliente aprovado?]               (Exclusive Gateway, US5)
   ↙               ↘
[Revisão manual]   [Publicar mensagem]
   ↓                  ↓
[Fim recusado]     [Fim aprovado]
```

## Pré-requisitos

- Backend e frontend rodando localmente (Setup/Foundational de `tasks.md`
  concluídos).
- Um serviço REST de teste disponível para a etapa de validação (pode ser um
  stub que retorna `APPROVED`/`REJECTED`).
- Um usuário com papel `WORKFLOW_DESIGNER` + `WORKFLOW_PUBLISHER`, e outro com
  `WORKFLOW_OPERATOR` + `TASK_USER` (podem ser o mesmo usuário no MVP).

## Passo a passo

1. **Desenhar** (US1): como Designer, criar um novo workflow no canvas com os
   6 nós acima, conectá-los e configurar cada um (formulário da tarefa
   humana, conector/timeout da chamada REST, condições do gateway).
   - Validar: `POST /api/workflows/{id}/validate` não retorna problemas.
2. **Publicar** (US1): `POST /api/workflows/{id}/publish`.
   - Validar: a resposta traz uma nova `version_number`; `GET
     /api/workflows/{id}/versions` lista essa versão.
3. **Iniciar execução** (US2): `POST
   /api/workflow-versions/{versionId}/instances`.
   - Validar: a instância avança sozinha até parar em `WAITING` na tarefa
     humana (não precisa de nenhuma ação manual para chegar até ali).
4. **Concluir a tarefa humana** (US3): como Task User, `GET /api/tasks`,
   `POST /api/tasks/{taskId}/claim`, preencher o formulário e `POST
   /api/tasks/{taskId}/complete`.
   - Validar: a instância retoma sozinha para a etapa de chamada REST.
5. **Observar a chamada externa** (US4): sem ação manual, a instância chama o
   serviço de teste e segue para o gateway.
   - Validar: `GET /api/instances/{instanceId}/history` mostra a etapa REST
     `COMPLETED` com o resultado nas variáveis.
6. **Observar a decisão** (US5): a instância segue o caminho `Publicar
   mensagem` (se o serviço de teste retornou `APPROVED`) ou `Revisão manual`
   (se retornou algo diferente).
   - Validar: para o caminho aprovado, um evento foi publicado (conferir no
     tópico/fila de teste ou na tabela `outbox_event` com `status =
     PUBLISHED`).
7. **Conferir o fim**: `GET /api/instances/{instanceId}` mostra `status =
   COMPLETED`, com histórico completo de todas as etapas em
   `/api/instances/{instanceId}/history`.

## Validação de operação (US6)

8. Repetir os passos 3-5 com o serviço REST de teste temporariamente retornando
   erro, para forçar uma etapa `FAILED` após esgotar as tentativas.
   - `POST /api/instances/{instanceId}/retry` deve reprocessar só a etapa
     falha, sem repetir as etapas já `COMPLETED` (conferir pelo `history`).
   - `POST /api/instances/{instanceId}/cancel` em outra instância em
     andamento deve marcá-la `CANCELLED`.
9. `GET /api/audit` deve conter uma entrada para cada publicação, início de
   execução, conclusão de tarefa, cancelamento e retry realizados acima, com
   autor e horário.

## Critério de sucesso do quickstart

Todos os 9 passos acima concluídos sem intervenção manual além da indicada
(claim/complete da tarefa humana, e as ações explícitas de retry/cancel do
passo 8) confirma que as user stories US1-US6 funcionam de ponta a ponta,
satisfazendo SC-001 a SC-008 do `spec.md`.
