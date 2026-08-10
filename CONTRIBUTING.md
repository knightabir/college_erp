# Contributing to College ERP

Thanks for your interest in contributing. This project is a modular-monolith Spring Boot +
React system with a fairly strict architecture (see `08-system-architecture.md` and
`07-coding-standards.md`), so please read this guide before opening a pull request — it will
save you a review round-trip.

## Before you start

1. Read `CLAUDE.md` at the repo root — it's the live build-progress log and states exactly what
   is implemented, what deliberately deviates from the planning docs, and what's still a stub.
   Don't re-derive or re-litigate a decision already recorded there.
2. Read the relevant planning document(s) for the area you're touching:

   | Doc | Contents |
   |---|---|
   | `01-project-vision.md` | Goals, guiding principles, phased module scope |
   | `02-prd.md` | Personas, user stories, functional/non-functional requirements |
   | `03-srs.md` | Formal functional requirements per module (`[MODULE-###]` codes) |
   | `04-database-schema.md` | Full schema, naming conventions, trigger design |
   | `05-api-documentation.md` | REST conventions and endpoint list per module |
   | `06-design-document.md` | Package layout, cross-module communication patterns |
   | `07-coding-standards.md` | Enforced style rules — **violations block PR merge** |
   | `08-system-architecture.md` | Tech stack, deployment modes, folder trees, ADRs |

3. If the module you're working on has no requirement/endpoint backing in these docs (several
   already-built modules don't — check `CLAUDE.md`'s per-module deviation notes), say so
   explicitly in your PR description rather than presenting an invented shape as spec.

## Getting set up

### Backend (`backend/college/`)

- JDK 21+
- A local PostgreSQL instance (see `application.yaml`'s `spring.datasource.*` /
  `spring.flyway.*` for the exact connection defaults and the `DB_*` / `FLYWAY_DB_*` env vars
  that override them)

```bash
cd backend/college
./mvnw compile
./mvnw test
./mvnw spring-boot:run
```

Schema is owned entirely by Flyway (`src/main/resources/db/migration`) — never set
`spring.jpa.hibernate.ddl-auto` to anything but `none`, and never edit a migration that has
already been released. Add a new `V{n}__{description}.sql` instead.

### Frontend (`frontend/`)

- Node.js LTS

```bash
cd frontend
npm install
npm run dev
npm run lint
npm run build
```

## Branching and commits

- Branch off `main`: `type/short-description`, e.g. `feat/library-fine-waiver`,
  `fix/attendance-shortfall-nulls`, `docs/update-api-doc`.
- Prefer [Conventional Commits](https://www.conventionalcommits.org/) style messages
  (`feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`) — makes history and future
  changelogs easier to scan, though it isn't mechanically enforced yet.
- Keep commits scoped to one logical change; avoid bundling unrelated modules in one commit.

## Code standards (non-negotiable — see `07-coding-standards.md` for full detail)

- **Module boundaries**: cross-module data access goes through the other module's *service*,
  never its repository. This is mechanically enforced by `ModuleBoundaryTest` (ArchUnit) — a
  build that fails this test will not be merged.
- **Controllers** are thin: `@Valid` + an explicit `@PreAuthorize` on every endpoint, no business
  logic, no repository access, DTOs only (never return entities).
- **Services**: constructor injection only (no field `@Autowired`), `@Transactional` on writes /
  `@Transactional(readOnly = true)` on reads, typed exceptions (never a raw `RuntimeException`),
  write to `AuditLogWriter` for every create/update/delete.
- **Entities**: all associations `FetchType.LAZY`, every `@Column` specifies
  `name`/`nullable`/`length`.
- Never return an unbounded `List` from a repository/list endpoint — paginate.
- Frontend: ES modules only, `@/` path alias, TanStack Query for server state, Zustand for UI
  state only (never server data in Zustand), named exports for shared components.

## Tests

Every behavioral change needs a test. Follow the existing per-module test naming
(`*ServiceTest`, `*ControllerTest`, etc.) and Mockito-based unit test style already in the
codebase.

```bash
# backend — run before every PR
cd backend/college
./mvnw test

# a single class or method
./mvnw test -Dtest=ClassName
./mvnw test -Dtest=ClassName#methodName

# frontend — run before every PR
cd frontend
npm run lint
npm run build
```

Both are also run automatically by CI (`.github/workflows/backend-ci.yml` and
`frontend-ci.yml`) on every pull request — a red CI check will block merge.

## Pull requests

1. Fill in the PR template completely — an empty template is a fast way to get your PR sent back.
2. Make sure `./mvnw test` (backend) and `npm run lint` / `npm run build` (frontend) pass locally
   for anything you touched, before pushing.
3. Keep PRs focused — one module/feature/bugfix per PR wherever reasonable.
4. Link the issue it closes, if any (`Closes #123`).
5. A maintainer will review; be ready to iterate. PRs are squash-merged by default to keep `main`
   history readable.
6. Do not edit a released Flyway migration, a file this repo documents as append-only/immutable
   (audit log, ledger entries, custody logs), or `CLAUDE.md`'s historical record of a past
   decision — add forward migrations/notes instead.

## Reporting bugs / requesting features

Use the issue templates under **Issues → New issue**. For security vulnerabilities, follow
`SECURITY.md` instead of filing a public issue.

## Code of conduct

Participation in this project is governed by `CODE_OF_CONDUCT.md`.
