# Feature Specification: Plataforma de Workflows (MVP)

**Feature Branch**: `001-workflow-platform-mvp`

**Created**: 2026-07-19

**Status**: Draft

**Input**: User description: "Plataforma de workflows: uma ferramenta que permite a um Designer criar, conectar, configurar, validar e publicar visualmente um fluxo de trabalho (workflow), e a um Operador iniciar e acompanhar execuções desse fluxo até a conclusão." (ver histórico completo da conversa para as 6 user stories detalhadas)

## Clarifications

### Session 2026-07-19

- Q: Qual a escala esperada de execuções simultâneas para este MVP? → A: Dezenas (piloto interno) — poucas dezenas de execuções simultâneas, uso interno/validação.
- Q: Qual o escopo de idioma/i18n da interface nesta primeira versão? → A: Só português (PT-BR), sem internacionalização nesta versão.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Desenhar e publicar um workflow (Priority: P1)

Como Designer, quero criar um workflow em uma área visual, adicionar etapas (início, fim, tarefa de usuário, chamada a serviço externo, decisão), conectá-las, configurar cada uma, validar o desenho e publicar uma versão, para que o workflow fique disponível para execução.

**Why this priority**: Sem a capacidade de desenhar e publicar um workflow, nenhuma outra funcionalidade da plataforma tem o que executar. É o pré-requisito de tudo.

**Independent Test**: Pode ser totalmente testado desenhando um workflow simples (início → uma etapa → fim), publicando-o e confirmando que uma versão imutável fica disponível — sem depender de execução.

**Acceptance Scenarios**:

1. **Given** um workflow com início, uma etapa e fim conectados e sem erros de validação, **When** o Designer publica, **Then** uma nova versão imutável fica disponível e pode ser referenciada por execuções futuras.
2. **Given** um workflow sem etapa de início, ou com uma etapa sem conexão, **When** o Designer tenta publicar, **Then** a publicação é bloqueada e os problemas são listados.
3. **Given** um workflow em edição, **When** o Designer salva sem publicar, **Then** o progresso é salvo como rascunho e pode ser retomado depois.

---

### User Story 2 - Executar um workflow do início ao fim (Priority: P1)

Como Operador, quero iniciar uma execução de uma versão publicada e acompanhar seu status até a conclusão, para confirmar que o fluxo definido realmente roda de ponta a ponta, com histórico de cada etapa executada.

**Why this priority**: Validar que um workflow publicado efetivamente executa é o segundo pré-requisito essencial — sem isso, publicar um workflow não tem valor demonstrável.

**Independent Test**: Pode ser totalmente testado publicando um workflow só com etapas automáticas (início, etapas automáticas, fim), iniciando uma execução e observando-a chegar ao fim sozinha, com histórico completo.

**Acceptance Scenarios**:

1. **Given** uma versão publicada só com início, etapas automáticas e fim, **When** o Operador inicia uma execução, **Then** a execução avança automaticamente por todas as etapas e termina com status concluído.
2. **Given** uma execução concluída, **When** o Operador consulta seu histórico, **Then** vê cada etapa executada com horário de início, horário de fim e resultado.

---

### User Story 3 - Concluir uma tarefa humana (Priority: P2)

Como usuário responsável por uma etapa de tarefa humana, quero ver as tarefas pendentes atribuídas a mim ou ao meu grupo, assumir uma tarefa, preencher um formulário e concluí-la, para que a execução do workflow continue a partir dali.

**Why this priority**: Amplia o motor de P1 para incluir interação humana, essencial para a maioria dos processos reais, mas depende de US1 e US2 já funcionarem.

**Independent Test**: Pode ser totalmente testado publicando um workflow com uma etapa de tarefa humana, iniciando uma execução, assumindo e concluindo a tarefa, e confirmando que a execução retoma sozinha.

**Acceptance Scenarios**:

1. **Given** uma execução parada aguardando uma tarefa humana, **When** o usuário assume a tarefa, preenche o formulário com os dados exigidos e a conclui, **Then** a execução retoma automaticamente para a próxima etapa.
2. **Given** um formulário com um campo obrigatório não preenchido, **When** o usuário tenta concluir, **Then** a conclusão é bloqueada com uma mensagem indicando o campo pendente.
3. **Given** uma tarefa atribuída a um grupo, **When** qualquer membro do grupo a assume, **Then** ela deixa de estar disponível para os demais membros do grupo.

---

### User Story 4 - Chamar um serviço externo (Priority: P2)

Como Designer, quero configurar uma etapa que chama um serviço REST externo (endereço, dados enviados, mapeamento do resultado, tempo limite e novas tentativas em caso de falha temporária), para integrar o workflow com sistemas existentes.

**Why this priority**: Sistemas reais dependem de integração com serviços externos; sem isso a plataforma só orquestra tarefas internas.

**Independent Test**: Pode ser totalmente testado publicando um workflow com uma etapa de chamada externa apontando para um serviço de teste, executando-o e confirmando que o resultado chega às variáveis do workflow.

**Acceptance Scenarios**:

1. **Given** uma etapa de chamada externa configurada corretamente, **When** a execução chega nessa etapa e o serviço externo responde com sucesso, **Then** o resultado é armazenado nas variáveis do workflow e a execução segue para a próxima etapa.
2. **Given** uma falha temporária de comunicação, **When** o número de novas tentativas configurado ainda não foi esgotado, **Then** o sistema tenta novamente automaticamente antes de marcar a etapa como falha.
3. **Given** uma falha que persiste além do número de tentativas configurado, **When** a última tentativa falha, **Then** a etapa é marcada como falha e fica disponível para reprocessamento manual.

---

### User Story 5 - Tomar uma decisão e publicar um evento (Priority: P3)

Como Designer, quero adicionar uma etapa de decisão que direciona a execução para caminhos diferentes de acordo com uma condição sobre os dados do workflow, incluindo um caminho que publica um evento para outros sistemas, para que o workflow ramifique seu comportamento e notifique terceiros.

**Why this priority**: Adiciona ramificação e notificação de terceiros — valioso, mas o MVP já demonstra valor de ponta a ponta sem isso (US1-US4 cobrem um fluxo linear completo).

**Independent Test**: Pode ser totalmente testado publicando um workflow com uma etapa de decisão de dois caminhos e confirmando que cada caminho é seguido conforme a condição avaliada, incluindo a publicação do evento no caminho aplicável.

**Acceptance Scenarios**:

1. **Given** uma etapa de decisão com duas condições de saída cobrindo os casos possíveis, **When** a execução avalia a condição, **Then** ela segue exatamente o caminho correspondente ao resultado.
2. **Given** um caminho de decisão que inclui publicação de evento, **When** a execução segue esse caminho, **Then** um evento observável por sistemas externos é publicado.

---

### User Story 6 - Operar as execuções em produção (Priority: P3)

Como Operador/Administrador, quero uma visão das execuções em andamento e concluídas, poder cancelar ou forçar nova tentativa de uma execução travada, e consultar métricas e auditoria (quem fez o quê e quando), para operar a plataforma com segurança e diagnosticar problemas.

**Why this priority**: Capacidades operacionais são essenciais para produção, mas o MVP pode ser demonstrado e validado (US1-US5) antes de precisar de operação madura.

**Independent Test**: Pode ser totalmente testado forçando uma falha em uma etapa, cancelando uma execução, forçando nova tentativa em outra, e confirmando que todas as ações aparecem na trilha de auditoria.

**Acceptance Scenarios**:

1. **Given** uma execução travada em uma etapa com falha, **When** o Operador solicita nova tentativa, **Then** a etapa é reprocessada a partir do ponto de falha, sem repetir etapas já concluídas com sucesso.
2. **Given** uma execução em andamento, **When** o Operador solicita o cancelamento, **Then** a execução para e é marcada como cancelada.
3. **Given** qualquer publicação, execução ou conclusão de tarefa, **When** a ação ocorre, **Then** ela fica registrada em uma trilha de auditoria consultável (quem, o quê, quando).

---

### Edge Cases

- O que acontece quando um workflow tem múltiplas versões publicadas e uma execução é iniciada sem indicar a versão? → assume-se a versão ativa mais recente (ver Assumptions).
- O que acontece com execuções já em andamento quando uma nova versão do mesmo workflow é publicada? → continuam na versão em que foram iniciadas (ver Assumptions).
- O que acontece quando o responsável por uma tarefa humana é removido do grupo antes de assumi-la?
- O que acontece quando uma chamada externa falha definitivamente após esgotar as tentativas configuradas?
- O que acontece quando dois usuários tentam assumir a mesma tarefa ao mesmo tempo? Apenas um deve conseguir; o outro recebe indicação de que a tarefa não está mais disponível.
- O que acontece quando os dados de uma execução não se encaixam em nenhuma condição configurada em uma etapa de decisão? A execução deve falhar de forma explícita nessa etapa, não seguir um caminho arbitrário.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema DEVE permitir que um Designer crie um workflow em uma área visual, adicionando e conectando etapas.
- **FR-002**: O sistema DEVE suportar, no mínimo, estes tipos de etapa: Início, Fim, Tarefa Humana, Chamada a Serviço Externo e Decisão.
- **FR-003**: O sistema DEVE permitir configurar as opções específicas de cada etapa (responsável/grupo para tarefa humana; conector/operação/dados para chamada externa; condições para decisão).
- **FR-004**: O sistema DEVE validar um workflow antes da publicação e bloquear a publicação quando houver problemas (ausência de início, etapa desconectada, decisão sem condições cobrindo os casos, tarefa humana sem formulário, etapa inalcançável, ausência de fim), listando todos os problemas encontrados de uma vez.
- **FR-005**: O sistema DEVE permitir salvar um workflow em edição como rascunho, sem publicá-lo.
- **FR-006**: O sistema DEVE publicar um workflow como uma versão imutável e numerada; uma vez publicada, a definição dessa versão NÃO PODE ser alterada.
- **FR-007**: O sistema DEVE permitir que um Operador inicie uma nova execução de qualquer versão publicada.
- **FR-008**: O sistema DEVE avançar uma execução automaticamente por etapas automáticas até encontrar uma tarefa humana, um erro não recuperável, ou o fim do workflow.
- **FR-009**: O sistema DEVE registrar, para cada etapa executada em uma instância, o horário de início, o horário de término e o resultado, disponibilizando esse histórico para consulta.
- **FR-010**: O sistema DEVE exibir a um usuário as tarefas pendentes atribuídas a ele individualmente ou a um grupo do qual participa.
- **FR-011**: O sistema DEVE permitir que um usuário assuma uma tarefa pendente, preencha o formulário da tarefa e a conclua.
- **FR-012**: O sistema DEVE validar os dados submetidos de um formulário de tarefa contra os campos obrigatórios definidos, bloqueando a conclusão com uma mensagem clara quando dados obrigatórios estiverem ausentes.
- **FR-013**: O sistema DEVE retomar automaticamente a execução a partir do ponto em que parou assim que uma tarefa humana pendente é concluída.
- **FR-014**: O sistema DEVE permitir configurar uma etapa de chamada externa com destino, dados enviados, tempo limite e número de novas tentativas para falhas temporárias.
- **FR-015**: O sistema DEVE armazenar o resultado de uma chamada externa bem-sucedida nas variáveis da execução e prosseguir para a próxima etapa.
- **FR-016**: O sistema DEVE tentar novamente automaticamente, até o limite configurado, quando ocorrer uma falha temporária de comunicação, antes de marcar a etapa como falha.
- **FR-017**: O sistema DEVE permitir configurar uma etapa de decisão com condições avaliadas sobre os dados da execução, cada condição determinando um caminho de saída distinto.
- **FR-018**: O sistema DEVE suportar, em pelo menos um caminho de uma etapa de decisão, a publicação de um evento observável por sistemas externos.
- **FR-019**: O sistema DEVE permitir que um Operador visualize todas as execuções (em andamento e concluídas) com seu status atual.
- **FR-020**: O sistema DEVE permitir que um Operador cancele uma execução em andamento ou force nova tentativa em uma etapa que falhou, retomando somente a partir da etapa falha, sem repetir etapas já concluídas com sucesso.
- **FR-021**: O sistema DEVE registrar toda publicação, início/conclusão de execução e conclusão de tarefa em uma trilha de auditoria consultável, capturando quem realizou a ação, qual foi a ação e quando ocorreu.
- **FR-022**: O sistema DEVE restringir cada ação ao papel apropriado: desenhar/editar rascunho (Designer), publicar (Publicador), iniciar/cancelar/forçar nova tentativa de execução (Operador), assumir/concluir tarefa (Usuário de Tarefa), administrar conectores e papéis (Administrador da Plataforma).
- **FR-023**: O sistema DEVE apresentar toda a interface e mensagens em português (PT-BR) nesta versão; suporte a outros idiomas está fora de escopo do MVP.

### Key Entities *(include if feature involves data)*

- **Workflow Definition**: o workflow em edição; representa o rascunho atual, que pode mudar livremente até ser publicado.
- **Workflow Version**: uma fotografia imutável e numerada de uma Workflow Definition no momento da publicação; é o que fica disponível para execução.
- **Workflow Instance (Execução)**: uma execução concreta de uma Workflow Version, com seu próprio status e dados.
- **Step Instance (Instância de Etapa)**: o registro de execução de uma etapa específica dentro de uma Instância, com status, tentativas, horários e resultado.
- **Human Task (Tarefa Humana)**: uma unidade de trabalho apresentada a um usuário ou grupo, associada a uma Step Instance e a um formulário.
- **Connector (Conector)**: uma integração configurada, usada por etapas de chamada externa.
- **Audit Entry (Registro de Auditoria)**: um registro de uma ação realizada por um usuário (publicação, início de execução, conclusão de tarefa, etc.), com autor e momento.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Um Designer consegue criar, validar e publicar um workflow simples de 3 etapas (início, uma etapa, fim) em menos de 10 minutos sem ajuda externa.
- **SC-002**: 100% dos workflows com problemas de desenho (início ausente, etapa inalcançável, decisão sem condições configuradas) têm a publicação bloqueada, com a lista completa de problemas apresentada de uma vez.
- **SC-003**: Para um workflow inteiramente automático, uma execução iniciada chega a um estado final com o histórico completo de cada etapa disponível, sem exigir nenhuma ação manual para mantê-la avançando.
- **SC-004**: Um usuário de tarefa consegue encontrar, assumir e concluir uma tarefa atribuída a ele, vendo a execução retomar automaticamente, sem precisar atualizar a tela manualmente mais de uma vez.
- **SC-005**: Ao menos 95% das falhas temporárias em chamadas externas se recuperam automaticamente (via nova tentativa) sem intervenção do Operador.
- **SC-006**: 100% das ações de publicação, início de execução, conclusão de execução e conclusão de tarefa aparecem na trilha de auditoria com autor e horário.
- **SC-007**: Um Operador consegue forçar nova tentativa em uma execução travada e recuperá-la sem perder os resultados das etapas já concluídas com sucesso.
- **SC-008**: O sistema suporta pelo menos algumas dezenas de execuções simultâneas (uso de piloto interno) sem degradação perceptível para Designers, Operadores ou Usuários de Tarefa.

## Assumptions

- Uma execução, quando não indica uma versão específica, sempre usa a versão publicada mais recente e ativa do workflow.
- Execuções já em andamento continuam na versão em que foram iniciadas quando uma nova versão do mesmo workflow é publicada — não há migração automática de execuções em andamento.
- Papéis de usuário (Designer, Publicador, Operador, Usuário de Tarefa, Administrador da Plataforma) são atribuídos a usuários por fora desta funcionalidade, por um Administrador da Plataforma; a administração de identidade/papéis em si é tratada apenas no nível necessário para restringir ações (FR-022), não como uma tela completa de gestão de usuários.
- "Grupo" para atribuição de tarefa humana corresponde a um conceito de grupo/identidade já existente fora desta funcionalidade.
- Autenticação padrão baseada em sessão/login é assumida; o protocolo específico de autenticação é uma decisão de planejamento técnico, não uma restrição desta especificação.
- Fora de escopo nesta primeira versão: subworkflows, execução paralela, compensações/transações distribuídas, alteração de um workflow já em execução, BPMN completo, scripts arbitrários no designer, marketplace de conectores, internacionalização (interface só em PT-BR).
- Escala alvo do MVP: algumas dezenas de execuções simultâneas (piloto interno), não carga de produção multi-cliente.
