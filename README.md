# Plataforma de Workflows (MVP)

Implementação da feature `001-workflow-platform-mvp` (ver `specs/001-workflow-platform-mvp/`),
derivada de `planejamento.md` via o fluxo speckit (`spec.md` → `plan.md` → `tasks.md`).

## Stack

- **Backend**: Java 17 (`backend/`, Maven, Spring Boot) — monólito modular com 9 pacotes de
  domínio (`definition`, `publication`, `runtime`, `humantask`, `connector`, `expression`,
  `identity`, `audit`, `shared`).
- **Frontend**: React + TypeScript + React Flow (`frontend/`, Vite).
- **Banco**: PostgreSQL local (18 nesta máquina; Flyway cuida das migrações, então 14+ funciona).

Interface só em português (PT-BR), sem framework de internacionalização (FR-023). Sem Docker —
tudo roda direto na máquina.

## Setup local

### Pré-requisitos

- JDK 17+ e Maven 3.9+
- Node.js 20+ e npm
- PostgreSQL instalado localmente (não via container)

### Banco de dados

Suba o PostgreSQL localmente (serviço do Windows já cuida disso após a instalação) e crie o banco
uma vez:

```sh
psql -h localhost -U postgres -c "CREATE DATABASE workflow_platform;"
```

As migrações Flyway rodam automaticamente quando a aplicação (ou os testes de integração) sobem.
Credenciais padrão em `application.yml`: usuário `postgres`, senha `postgres` — ajuste via
`DB_USERNAME`/`DB_PASSWORD` se o seu Postgres local usa outras.

### Backend

```sh
cd backend
mvn spring-boot:run
```

API em `http://localhost:8080/api`. Requer um provedor OpenID Connect configurado via
`OIDC_ISSUER_URI` (padrão: `http://localhost:8081/realms/workflow-platform`) para autenticar
requisições de verdade — a aplicação sobe sem ele (confirmado), mas toda rota exige um JWT válido
(`401` sem token).

```sh
mvn test        # testes que não precisam de banco (rápidos, sempre rodam)
mvn verify       # inclui os testes de integração contra o Postgres local (ver abaixo)
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

## Testes: unitários vs. integração

Os testes são separados por uma tag JUnit (`@Tag("requires-postgres")`), não pelo nome do
arquivo — vários `*IntegrationTest` não precisam de banco (usam mocks ou um `HttpServer` local) e
continuam rodando em `mvn test`. Só os que realmente batem no Postgres (`PublicationIntegrationTest`,
`SequentialExecutionIntegrationTest`, `HumanTaskResumeIntegrationTest`) ficam reservados para
`mvn verify` (Failsafe), e são `@Transactional` para desfazer os dados ao final de cada teste (o
banco é real e persistente, não um container efêmero descartado a cada rodada).

O CI (`.github/workflows/ci.yml`) roda só `mvn test`, já que os runners do GitHub Actions não têm
um Postgres configurado para este projeto.

## Nota sobre o ambiente em que este MVP foi implementado

Este ambiente de desenvolvimento tem JDK 24 instalado (mais novo que os 17/21 LTS recomendados).
Isso teve dois efeitos práticos, documentados em `specs/001-workflow-platform-mvp/research.md`:

- **Lombok foi removido do backend**: o Lombok gerenciado pelo Spring Boot 3.3 não suporta o
  compilador do JDK 24 instalado aqui. As entidades/DTOs usam getters/construtores explícitos.
- **Mockito não mocka classes concretas neste ambiente** (só interfaces) — os testes evitam
  `mock()` em `@Service` concretos, usando instâncias reais com dependências de interface
  mockadas.

Nenhuma dessas duas coisas é uma limitação da arquitetura em si — rodar em um JDK 17/21 LTS padrão
remove as duas restrições.

Docker foi avaliado para Testcontainers/PostgreSQL em container, mas descartado neste projeto —
ver `specs/001-workflow-platform-mvp/research.md` (decisão #7) para o histórico. O projeto roda
inteiramente contra um PostgreSQL instalado localmente.
