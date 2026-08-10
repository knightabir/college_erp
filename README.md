# College ERP

A multi-tenant College Enterprise Resource Planning (ERP) system built as a modular monolith —
one Spring Boot backend, one PostgreSQL database, one React frontend — designed to run equally
well as a single-college on-prem/air-gapped deployment or a multi-tenant SaaS platform.

## Overview

College ERP covers the full academic and administrative lifecycle of a college: admissions,
academics, attendance, examinations, finance, library, assets, HR, hostel, transport,
communication, placements, and platform administration — all behind one consistent REST API and
a shared RBAC/audit/notification kernel.

The system is specified in depth across eight planning documents at the repo root before any
corresponding code is written:

| Doc | Contents |
|---|---|
| [`01-project-vision.md`](01-project-vision.md) | Problem statement, goals, guiding principles, phased module scope |
| [`02-prd.md`](02-prd.md) | Personas, user stories, functional/non-functional requirements, permission matrix |
| [`03-srs.md`](03-srs.md) | Formal functional requirements per module, external interfaces, data rules |
| [`04-database-schema.md`](04-database-schema.md) | Full table-by-table schema, naming conventions, triggers, indexes |
| [`05-api-documentation.md`](05-api-documentation.md) | REST API conventions and full endpoint list per module |
| [`06-design-document.md`](06-design-document.md) | Package layout, cross-module communication, security/data-flow design |
| [`07-coding-standards.md`](07-coding-standards.md) | Enforced Java/SQL/JS style rules — violations block PR merge |
| [`08-system-architecture.md`](08-system-architecture.md) | Architecture overview, tech stack, deployment modes, folder trees, ADRs |

`CLAUDE.md` tracks exactly how far the implementation has gotten against that spec, including
every deliberate deviation and every still-open gap — treat it as the up-to-date build log.

## Architecture

- **Style:** modular monolith (not microservices) — one deployable JAR, strictly partitioned into
  feature modules, one PostgreSQL database. Chosen so the system can be deployed on an air-gapped
  campus network and so cross-module operations (e.g. fee collection) get real ACID transactions
  instead of sagas.
- **Stack:** React SPA (static, served by Nginx) → Spring Boot 3.3.5 (Java 21) → PostgreSQL.
- **Multi-tenancy:** shared schema + `tenant_id` + Postgres Row-Level Security, with a
  least-privilege `erp_app` runtime DB role (Postgres exempts superusers from RLS, so the app
  never connects as one). Air-gapped/on-prem deployments run the identical codebase with a single
  fixed tenant row — no code branching between modes.
- **Security:** JWT access/refresh tokens carrying no roles or permissions (so revocation is
  immediate, not token-lifetime-bound); three-layer permission evaluation (role template →
  scoped grant → deny); every endpoint requires an explicit authorization check.
- **Data integrity:** UUID primary keys, soft delete everywhere, append-only/immutable tables for
  audit log and financial/custody ledgers (enforced by DB triggers, not just app code), `NUMERIC`
  for money, `TIMESTAMPTZ` for all timestamps.

See `08-system-architecture.md` for the full architecture, ADRs, and folder trees, and
`CLAUDE.md` for what of this is actually built versus still spec.

## Repository layout

```
college_erp/
├── backend/college/     # Spring Boot 3.3.5 (Java 21) modular monolith
├── frontend/            # React SPA (Vite, React 19, Tailwind, TanStack Query, Zustand)
├── 01-project-vision.md … 08-system-architecture.md   # planning/spec docs
└── CLAUDE.md             # live build-progress log and deviation notes
```

## Backend

**Location:** `backend/college/`

Prerequisites: JDK 21+, a running PostgreSQL instance matching `application.yaml`'s
`spring.datasource.*` (overridable via `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` env vars).

```bash
cd backend/college
./mvnw compile                            # compile
./mvnw test                               # unit tests (JUnit 5 + Mockito)
./mvnw test -Dtest=ClassName               # single test class
./mvnw test -Dtest=ClassName#methodName    # single test method
./mvnw verify                             # unit + integration tests
./mvnw spring-boot:run                    # run locally
```

Schema is owned entirely by Flyway migrations (`src/main/resources/db/migration`) —
`spring.jpa.hibernate.ddl-auto` is `none`. Migrations apply automatically on startup.

Implemented modules (see `CLAUDE.md` for exact status and deviations per module): platform kernel
(`common`, `config`, `web`, `security`, `audit`, `notification`, `scheduler`, `storage`,
`tenant`), `auth`, `academic`, `student`, `attendance`, `examination`, `finance`, `library`,
`asset`, `hr`, `hostel`, `transport`, `communication`, `admin`, `admissions`, `placement`.

## Frontend

**Location:** `frontend/`

Prerequisites: Node.js (LTS) and a package manager (`pnpm` per the coding standards; `npm`
scripts also work as configured).

```bash
cd frontend
npm install
npm run dev        # start the dev server
npm run build       # production build
npm run lint         # ESLint
npm run preview      # preview a production build locally
```

Stack: React 19, Vite, Tailwind CSS, TanStack Query for server state, Zustand for UI-only state,
React Router, shadcn-based UI components.

## Development conventions

Full detail lives in `07-coding-standards.md`; the essentials:

- Controllers are thin — no business logic, no direct repository access, DTOs only.
- Services use constructor injection, explicit transaction boundaries, typed exceptions, and
  write to the audit log on every create/update/delete.
- Cross-module access always goes through the other module's service, never its repository
  (mechanically enforced by an ArchUnit test).
- All associations are lazily fetched; all list endpoints are paginated.
- Flyway migrations are sequential, one concern per file, and never edited once released.

## License

No license file has been added yet — treat this repository as all-rights-reserved until one is
added.
