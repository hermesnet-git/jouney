# Plataforma de Workflows (MVP)

Implementação da feature `001-workflow-platform-mvp` (ver `specs/001-workflow-platform-mvp/`),
derivada de `planejamento.md` via o fluxo speckit (`spec.md` → `plan.md` → `tasks.md`).

## Stack

- **Backend**: Java 17 (`backend/`, Maven, Spring Boot) — monólito modular com 9 pacotes de
  domínio (`definition`, `publication`, `runtime`, `humantask`, `connector`, `expression`,
  `identity`, `audit`, `shared`).
- **Frontend**: React + TypeScript + React Flow (`frontend/`, Vite).
- **Banco**: PostgreSQL 16 (Flyway para migrações).

Interface só em português (PT-BR), sem framework de internacionalização (FR-023).

## Setup local

### Pré-requisitos

- JDK 17+ e Maven 3.9+
- Node.js 20+ e npm
- Docker (para PostgreSQL local e para os testes de integração com Testcontainers)

### Banco de dados

```sh
docker compose up -d
```

Sobe um PostgreSQL em `localhost:5432` (`workflow_platform`/`workflow_platform`/`workflow_platform`).

### Backend

```sh
cd backend
mvn spring-boot:run
```

API em `http://localhost:8080/api`. Requer um provedor OpenID Connect configurado via
`OIDC_ISSUER_URI` (padrão: `http://localhost:8081/realms/workflow-platform`) — os testes de
integração e o `mvn compile`/`mvn test-compile` não precisam disso, só rodar a aplicação de
verdade precisa.

```sh
mvn test        # roda os testes que não precisam de Docker
mvn verify       # inclui os testes de integração (Testcontainers) — precisa de Docker rodando
mvn spotless:apply   # formata o código (Google Java Format)
```

### Frontend

```sh
cd frontend
npm install
npm run dev      # http://localhost:5173
npm run build
npm run test
npm run lint
```

## Estrutura

Ver `specs/001-workflow-platform-mvp/plan.md` (Structure Decision) para o mapeamento completo de
diretórios, e `tasks.md` para o histórico task-a-task da implementação.

## Nota sobre o ambiente em que este MVP foi implementado

Este ambiente de desenvolvimento tem JDK 24 instalado (mais novo que os 17/21 LTS recomendados) e
não tem Docker/PostgreSQL pré-configurados. Isso teve dois efeitos práticos, documentados em
`specs/001-workflow-platform-mvp/research.md`:

- **Lombok foi removido do backend**: o Lombok gerenciado pelo Spring Boot 3.3 não suporta o
  compilador do JDK 24 instalado aqui. As entidades/DTOs usam getters/construtores explícitos.
- **Mockito não mocka classes concretas neste ambiente** (só interfaces) — os testes evitam
  `mock()` em `@Service` concretos, usando instâncias reais com dependências de interface
  mockadas.

Nenhuma dessas duas coisas é uma limitação da arquitetura em si — rodar em um JDK 17/21 LTS
padrão remove as duas restrições.

Testes de integração (Testcontainers, arquivos `*IntegrationTest.java` e
`ConcurrentExecutionLoadTest.java`) foram escritos e compilam, mas não foram executados nesta
sessão por falta de Docker disponível — rode `mvn verify` com Docker ativo para validá-los.
