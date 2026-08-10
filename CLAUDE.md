# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Current State of the Repository — Read This First

Implementation is underway. **Done and passing tests:** the platform kernel (`common`, `config`,
`web`, `security`, `audit`), `module/auth`, `module/academic`, `module/student`,
`module/attendance`, `module/examination`, `module/finance`, `module/library` (catalog,
barcode-driven circulation, custody chain, reservations, fines), `module/asset` (registry with
auto-generated barcode tags, append-only custody-log transfers, maintenance tickets, disposal,
on-demand depreciation), the platform's `notification`/`scheduler` kernel (in-app inbox + SSE
push + stubbed email/SMS channels, a `background_jobs` poller with retry/backoff, and four of the
five [SCHED-004] cron jobs actually wired to real data), `module/hr` (staff profiles, leave
types/balances/requests with apply→approve/reject and balance enforcement), and
`module/hostel`+`module/transport` (rooms/allocations/outpass, routes/vehicles/route mappings —
**built from schema alone, no SRS/API basis**), `module/communication` (notices,
apply/assign/resolve/close helpdesk tickets with comments), and `module/admin` (user management,
audit log search — reports/system-health deferred, see below), the `storage` kernel
(`FileStorageService` interface + local-disk implementation + `GET /files/{fileId}`), and
`module/admissions`+`module/placement` (applications→review→merit-list→seat-allotment→
convert-to-student; companies→drives→applications→interviews→offers) — see "Build progress" below
for exactly what exists and what's still a stub. **This means every module from `01-project-
vision.md`'s Phase 1 + Phase 2 scope is now built; only the `frontend` remains entirely unstarted**
(still empty — no `package.json`, no source files).

**This system is now multi-tenant (a SaaS platform any number of colleges can share), added at the
user's explicit request.** `08-system-architecture.md` §5.2 had named this as a one-paragraph
future "cloud mode" aspiration since the very first architecture pass, but zero DDL/FRs/endpoints
ever backed it anywhere else — see V017's own header comment and the `tenant` kernel package's
deviations section far below for the full design (shared schema + `tenant_id` + Postgres Row-Level
Security, a public self-service `POST /api/v1/register` "register your college" endpoint, and — the
one genuinely new piece of infrastructure — a least-privilege `erp_app` runtime DB role, because
Postgres unconditionally exempts superusers from their own RLS policies and this project's own dev
setup connects as one). Retrofitted onto the three reference modules only so far — **`auth`,
`academic`, `student`** — matching this session's own "foundation + reference modules now, rest
incrementally" scope decision; the other 13 modules' tables have no `tenant_id` column and are not
tenant-isolated yet. Follow the exact pattern in the deviations section below when converting each
of them in turn.

**`module/admissions` and `module/placement` are a different kind of build than everything else in
this file — their entire schema is invented, not sourced from the docs.** Unlike every other
module, these two have **zero DDL in `04-database-schema.md`, zero requirements in `03-srs.md`,
and zero endpoints in `05-api-documentation.md`** — only a file tree listing in
`08-system-architecture.md` (Application, MeritList, SeatAllotment, LateralEntryApplication,
Company, PlacementDrive, InterviewRound, PlacementOffer, ...). This session initially deferred
both modules for exactly that reason (see git history / earlier revisions of this file), but built
them at the user's **explicit request to complete the full backend before moving to the
frontend**, overriding that default caution. `V015`/`V016`'s header comments carry the full
disclosure and every domain class/enum is marked `INVENTED` in its own javadoc. **Treat this
schema as this session's best-effort placeholder, not an authoritative spec** — if
`04-database-schema.md`/`03-srs.md`/`05-api-documentation.md` ever gain a real Admissions &
Placement section, that supersedes everything in `V015`/`V016` and the entities/services/
controllers described below, not the other way around. See the module's own deviations section
far below for the specific modeling choices made and why.

Everything described below as "the architecture" is the **target design** recorded in the eight
planning documents at the repo root; treat unbuilt modules (currently just the frontend) as spec,
not code.

### Build progress

| Area | State |
|---|---|
| Kernel (common/config/web/security/audit) | Built, unit-tested (`JwtTokenProviderTest`, `PermissionEvaluatorTest`) |
| `module/auth` | Built, unit-tested (`AuthServiceTest`) — login, refresh, logout, change-password |
| `module/academic` | Built, unit-tested (`DepartmentServiceTest`, `BatchServiceTest`, `TimetableConflictTest`) — see below for deliberate deviations from the docs' exact file list |
| `module/student` | Built, unit-tested (`StudentServiceTest`, `EnrollmentServiceTest`) — students, enrollment (incl. section transfer), guardians, documents — see below for deviations |
| `module/attendance` | Built, unit-tested (`AttendanceRulesEngineTest`, `AttendanceCaptureServiceTest`, `CondonationServiceTest`) — sessions, marking, summaries, defaulters, condonation, all against the [ATT-001]–[ATT-007] rules in 03-srs.md §3.5 |
| `module/examination` | Built, unit-tested (`GradingScaleTest`, `MarksEntryServiceTest`, `ResultProcessingTest`) — exams, hall-ticket generation w/ attendance eligibility, marks entry→submit→moderate→publish, SGPA/CGPA, revaluation — see below for what's deferred |
| `module/finance` | Built, unit-tested (`FeeCollectionServiceTest`, `LedgerServiceTest`, `SegregationOfDutiesTest`, `FinancialYearLockTest`) — fee structures→invoices→payments, double-entry ledger, financial year dual-approval close, accountant grants, vendors/POs — see below for what's deferred and one kernel-level change this module required |
| `module/library` | Built, unit-tested (`CirculationServiceTest`, `CustodyLogTest`, `FineAccrualTest`) — catalog + ZXing Code 128 barcodes (generated on demand, never persisted), issue/return/renew against [LIB-004]/[LIB-005]/[LIB-008], reservation queue, fine waiver reusing the kernel's 3-layer RBAC directly — see below for one enforcement gap |
| `module/asset` | Built, unit-tested (`AssetServiceTest`, `AssetTransferServiceTest`, `AssetCustodyLogTest`, `MaintenanceServiceTest`, `DepreciationServiceTest`) — registration w/ auto-generated `{CATEGORY_PREFIX}-{seq}` tags + on-demand ZXing barcode, fully-immutable append-only custody-log transfers ([AST-002]), maintenance tickets ([AST-003]), disposal ([AST-004]), on-demand straight-line depreciation ([AST-005]) — see below for deviations |
| `notification`/`scheduler` kernel | Built, unit-tested (`NotificationServiceTest`, `TemplateRendererTest`, `BackgroundJobTest`, `JobDispatcherTest`, `AttendanceShortfallScanJobTest`, `FeeDueDateReminderJobTest`, `LibraryOverdueScanJobTest`, `PermissionGrantExpiryJobTest`) — in-app inbox + SSE push + config-gated (off by default) stub email/SMS channels, `background_jobs` poller (`FOR UPDATE SKIP LOCKED`, exponential backoff, nightly DONE-job cleanup), 4 of 5 [SCHED-004] cron jobs wired to real module data, [ATT-007]/[LIB-007]'s previously-WARN-only alerts now dispatch for real — see below for what's deferred |
| `module/hr` | Built, unit-tested (`StaffServiceTest`, `LeaveBalanceServiceTest`, `LeaveRequestServiceTest`) — staff profiles ([HR-001]), leave types, idempotent per-year balance initialization ([HR-002]), apply→approve/reject with balance enforcement and cancel-releases-balance ([HR-003]/[HR-004]) — see below for deviations |
| `module/hostel` + `module/transport` | Built, unit-tested (`RoomAllocationServiceTest`, `OutpassRequestServiceTest`, `StudentRouteMappingServiceTest`) — hostel blocks/rooms/allocations (capacity-enforced) + outpass apply/approve/reject/cancel; routes/vehicles/student-route assignment (one active route per student) — **the only modules built so far with zero 03-srs.md requirements and zero 05-api-documentation.md endpoints to conform to**, see below |
| `module/communication` | Built, unit-tested (`NoticeServiceTest`, `NoticeTest`, `HelpdeskServiceTest`) — notices (publish-immediately, ALL/DEPARTMENT/BATCH/ROLE targeting), helpdesk tickets (raise→assign→start-progress→resolve→close, comments restricted to owner/assignee) against [COMM-001]/[COMM-002] — see below for deviations |
| `module/admin` | Built, unit-tested (`UserManagementServiceTest`, `AuditLogServiceTest`, `BootstrapAdminRunnerTest`) — user create/list/get/lock/unlock, role assignment (self-assignment blocked, FR-RBAC-3), audit log search across user/entity-type/action/date-range filters, startup bootstrap of the system's first `SUPER_ADMIN` account (`BootstrapAdminRunner`/`UserManagementService.bootstrapSuperAdminIfMissing()`, since there is no public registration endpoint and every user-creation path otherwise requires an already-authenticated admin) — see below for scope/deviations. **Reports (02-prd.md §9) and `SystemHealthController` are not built** — Reports needs a PDF/Excel export library not in `pom.xml` plus cross-cutting aggregation over every module (same "no rendering, deferred" treatment as `GradeCardService`/PDF and `module.storage`); `SystemHealthController` has zero backing text anywhere outside the file tree. |
| `module/admissions` + `module/placement` | Built, unit-tested (`ApplicationWorkflowServiceTest`, `MeritProcessingServiceTest`, `EnrollmentConversionServiceTest`, `PlacementDriveServiceTest`, `PlacementAnalyticsServiceTest`) — **entire schema INVENTED, see the header note above and the module's own deviations section below before touching either module.** Admissions: apply → single-stage review (with an append-only workflow-step audit trail) → document upload/verify → merit list generation (ranked by qualifying exam score) → seat offer/accept/decline → convert-to-student (creates a real `module.student` Student + Enrollment). Placement: company/drive CRUD → student applies → interview rounds (auto-incrementing, a REJECTED result auto-rejects the application) → offer → accept/decline, plus a per-drive analytics rollup. |
| `V001`–`V018` migrations | Applied via Flyway — **verified against a real, live Postgres instance**, not just "should apply" (see the `tenant` kernel row/deviations section below for the exact live-verification steps; this superseded the former "untested against a live DB" caveat, which no longer applies to any migration). V003 also backfills the `role_assignments.department_id` / `permission_grants.scope_department_id`+`scope_batch_id` FKs V001 deliberately left off; V009 backfills `assets.department_id` / `asset_custody_log.to_department_id` the same way; V010 (notification/scheduler), V011 (HR), V012 (hostel/transport), V013 (communication) have no deviations from 04-database-schema.md §16/§13/§14/§15. `module/admin` needed no new migration. V015 (admissions)/V016 (placement) are **entirely invented** — see the header note above; V014 is deliberately skipped (reserved for 04-database-schema.md §19's seed data). V017 is the multi-tenancy foundation — entirely invented too, see the `tenant` kernel deviations section below. **V018 is a live-bug fix for 13 of V017's RLS policies** (`invalid input syntax for type uuid: ""`, surfaced by `AttendanceShortfallScanJob` querying `subject_offerings` with no tenant context) — see the `tenant` kernel deviations section below for the root cause and fix. |
| ArchUnit module boundary tests | Built (`ModuleBoundaryTest`, `archunit-junit5` 1.4.1) — mechanically enforces "no cross-module repository access" (07-coding-standards.md §4.4), "controllers never depend on a repository directly", and "no field-level `@Autowired`". Confirmed working against a real, deliberately-injected violation before trusting a clean run (see below) — passes cleanly except the one documented `module.admin → module.auth` exception, which is allowlisted by name. Still passes clean after admissions/placement were added. |
| `storage` kernel | Built, unit-tested (`LocalFileStorageServiceTest`) — `FileStorageService` interface, `local` mode implementation (`erp.storage.mode`, default), `GET /files/{fileId}`. `minio`/`s3` modes are **not implemented** — see below. No module has been wired to actually call `store()` yet (student documents, asset/library photos, etc. still don't persist uploads) — that's a follow-up integration pass, not part of this kernel piece. |
| PDF generation | **Not built.** [EXAM-006]'s PDF grade card doesn't exist — `GradeCardService` returns the same structured data a PDF would contain (see below), no rendering. |
| TOTP (FR-AUTH-4) | **Not implemented.** A user with `totp_enabled = true` fails login outright (`AuthService.loginTotpGuard`) rather than silently skipping 2FA — fail closed, not a silent bypass. |
| `security/session/SessionRegistry`, `SessionManagementService`, `TotpController` | **Not built** — deferred, not required for login/refresh/logout to work |
| `RoleTemplate` (Layer-1 role→permission defaults) | Only `SUPER_ADMIN`/`ADMIN` seeded (wildcard). Every other role has no unconditional grants yet — see the class javadoc: the canonical "Roles & Permissions" doc PRD §6 references isn't in this repo. `UserRole` similarly only has the 21 roles explicitly named across the 8 docs, not the "24" the architecture doc's file-tree comment implies. |
| `config/OpenApiConfig.java`, `springdoc-openapi` dependency | **Not added** — skipped until API surface is large enough to matter |
| `tenant` kernel (multi-tenancy) | Built, unit-tested (`UserManagementServiceTest`'s new `createTenantOwner`/`bootstrapTenantAndSuperAdminIfMissing` cases, `BootstrapAdminRunnerTest`) and **verified live against a real Postgres instance** (see its own deviations section below for the exact curl sequence) — `Tenant` entity/repository, `TenantContext` (ThreadLocal), `TenantAwareDataSource` (Postgres RLS session-variable-per-connection-checkout), `TenantFilter`, public `POST /api/v1/register` "register your college" endpoint, and the `erp_app` least-privilege runtime DB role (V017). Retrofitted onto `module/auth` (app-level scoping, no RLS — see below), `module/academic` + `module/student` (real Postgres RLS, all 13 tables) — **the other 13 modules are not tenant-aware yet**, see the top-of-file callout and this section's own notes for the exact pattern to repeat. |
| Everything else in the module list (§10-12 of `08-system-architecture.md`) | Not started |

`tenant` kernel (multi-tenancy) deviations from the docs — **read this before touching any of
`auth`/`academic`/`student`'s entities, services, or migrations, or before converting another
module to be tenant-aware**:
- **Every planning doc but one is silent on multi-tenancy.** `08-system-architecture.md` §5.2 named
  "Multi-Tenancy (Cloud Mode)" in one paragraph (shared schema, Postgres RLS, a `TenantFilter`, an
  `app.current_tenant` session variable) and nowhere else — `04-database-schema.md` has zero
  `tenant_id` columns or `tenants` table anywhere, `03-srs.md` defines "Tenant" only in its glossary
  with no `[TENANT-xxx]` FRs, `02-prd.md` never mentions it, `05-api-documentation.md` documents no
  registration endpoint. V017/`Tenant`/`RegisterCollegeRequest`'s own header comments and javadocs
  all disclose this — treat every field/endpoint/table below as this session's best-effort design,
  not a transcribed spec, same posture as `module/admissions`+`module/placement`.
- **Isolation strategy: shared schema + `tenant_id` column + Postgres Row-Level Security** — chosen
  over schema-per-tenant or database-per-tenant specifically because it's what §5.2 already
  sketched, and because it preserves ADR-001's single-JAR/single-DB deployment story (air-gap mode
  is simply "exactly one tenant row exists" — see `BootstrapAdminRunner` below).
- **Two different tenant-scoping mechanisms, deliberately, for two different reasons**:
  - `module.academic` + `module.student` (13 tables) get **real Postgres RLS** — `tenant_id` NOT
    NULL, `ENABLE ROW LEVEL SECURITY`, a `USING (tenant_id = current_setting('app.current_tenant',
    true)::UUID) WITH CHECK (...)` policy. There is no legitimate cross-tenant read case for any of
    this data, so the database refuses a wrong-tenant row outright, independent of whether every
    Java code path remembered to filter — verified live: a direct `GET .../departments/{id}` by a
    different tenant's known UUID returns 404, even though `DepartmentService.getEntity()` has no
    tenant clause in its query at all (see this file's git history / the session transcript for the
    exact curl proof).
  - `module.auth`'s `users`/`role_assignments` get a `tenant_id` **column only, no RLS** — enforced
    in `UserManagementService` application code instead (reads `TenantContext.getCurrentTenantId()`
    ambiently, same ThreadLocal-read pattern `AuditLogWriter` already uses for
    `SecurityContextHolder`/`MDC` rather than threading a "current tenant" parameter through every
    method signature). Reason: `AuthService.login()` resolves a user by username/email **alone**,
    with no "which college" selector on the login form — `users.username`/`users.email` stay
    globally unique across the whole platform specifically so that lookup stays unambiguous. RLS
    keyed on `tenant_id` would make that lookup return zero rows before the tenant is even known,
    breaking login outright. `permission_grants`/`refresh_tokens` were left untouched entirely in
    this pass (no `tenant_id` at all) — refresh tokens are looked up by unique hash regardless of
    tenant (no real gap), and permission_grants is a Layer-2 RBAC feature orthogonal to this pass's
    scope; both are natural follow-ups when converting `module.finance`, which already has its own
    `accountant_permission_grants` table with the identical shape.
- **`current_setting('app.current_tenant', true)` uses the two-argument form everywhere** — the
  `true` "missing_ok" flag makes it return `NULL` instead of raising an error when unset (e.g. a
  connection Flyway/the bootstrap runner used, or any future code path that forgets to establish
  tenant context). `tenant_id = NULL` is never true in SQL, so such a connection sees **zero rows**
  on every RLS-protected table — a deliberate fail-closed default, not an oversight.
- **RLS only actually restricts non-superuser, non-owning roles — Postgres has no override for
  superusers,** and this project's own documented local dev credentials (`DB_USERNAME=postgres`,
  set up per this session's own earlier turn) are exactly that. Discovered live, not theoretically:
  RLS policies alone were silently inert against the `postgres` role. Fixed by V017 creating a new,
  deliberately non-superuser, non-table-owning `erp_app` role and splitting the app's own runtime
  connection from Flyway's:
  - `spring.datasource.*` (JPA/Hibernate, via `TenantDataSourceConfig`'s `TenantAwareDataSource`
    wrapper) → connects as `erp_app` (`DB_USERNAME`/`DB_PASSWORD`, defaults `erp_app`/`ErpApp@2026`
    — **change both before any non-local deployment**, same as every other checked-in default
    credential in this project).
  - `spring.flyway.url/user/password` (a separate, standard Spring Boot Flyway property group) →
    still connects as the superuser (`FLYWAY_DB_USERNAME`/`FLYWAY_DB_PASSWORD`, defaults
    `postgres`/`admin`, matching this project's existing local Postgres login) — migrations
    (including V017 itself, which needs `CREATE ROLE`/`GRANT`) need that elevated connection; normal
    request traffic must not have it. Verified live: after this split, `spring-boot:run` connects
    successfully as `erp_app` immediately following Flyway's `postgres`-run migration, with zero
    manual setup steps — V017 creates the role, grants it access to every existing table (plus
    `ALTER DEFAULT PRIVILEGES` so future migrations' tables are usable automatically too), and the
    app's own datasource bean picks it straight up.
- **`TenantAwareDataSource`** (`DelegatingDataSource` wrapper) is the mechanism that makes RLS work
  under connection pooling at all — HikariCP does **not** reset session-level state between
  checkouts, so without this, a connection that served one tenant's request could still carry that
  tenant's `app.current_tenant` when handed to a completely different tenant's request next. Every
  `getConnection()` call (fresh or reused, indistinguishable from here) immediately `SET`s or
  `RESET`s the session variable from `TenantContext` before the connection is used for anything.
- **`V018` fixes a live bug in V017's RLS policies, found via `AttendanceShortfallScanJob`**:
  `RESET app.current_tenant` does not reliably make `current_setting(..., true)` return `NULL`,
  contrary to `TenantAwareDataSource`'s original javadoc — `app.current_tenant` is a custom
  (placeholder) GUC, and once *any* prior checkout on a given pooled connection has `SET` it at
  least once, a later `RESET` on that same physical connection settles it to `''` (empty string)
  instead, for the rest of that connection's life. A scheduled job (no tenant context at all)
  querying `subject_offerings` on a connection that had just served a tenant-scoped request hit
  exactly this: `ERROR: invalid input syntax for type uuid: ""`, because every policy cast
  `current_setting('app.current_tenant', true)::UUID` directly. `V018` wraps the read side of all
  13 affected policies in `NULLIF(current_setting('app.current_tenant', true), '')` before the
  `::UUID` cast, so both "never set" and "reset after being set" collapse to `NULL` before
  casting — same fail-closed `tenant_id = NULL` result, no more thrown exception. Not just a
  scheduler-only risk: any connection reuse into a tenant-less code path (public endpoints,
  future jobs, etc.) after serving a tenant-scoped request could have hit this. Keep the `NULLIF`
  wrapper in any future policy written against `app.current_tenant`.
- **No JWT change at all** — tenant id is **not** embedded in the access token, deliberately
  matching ADR-004's existing reasoning (roles/permissions are re-resolved from the DB on every
  request rather than cached in the token so a revocation takes effect immediately). `UserPrincipal`
  gained a `tenantId` field, populated the same way roles already are —
  `UserDetailsServiceImpl.toPrincipal()` reads `user.getTenantId()` fresh from the DB on every
  request. `TenantFilter` (registered via `SecurityConfig#addFilterAfter(..., JwtAuthenticationFilter
  .class)`, same "explicit wiring, not `@Component`" convention as `JwtAuthenticationFilter` itself)
  just copies that onto `TenantContext` for the rest of the request.
- **Plain `@Builder` doesn't see superclass fields** (07-coding-standards.md §2.2's existing
  "id/timestamps aren't set through the builder" convention already relies on this same Lombok
  limitation) — `tenantId` lives on the new `common.domain.TenantScopedEntity` (a `BaseEntity`
  subclass, see its own javadoc for why it's a separate class rather than adding the column to
  `BaseEntity` directly: that would make Hibernate expect `tenant_id` on every one of the other 13
  modules' tables immediately, breaking them). Every `*Service.create()` method in `academic`/
  `student` therefore sets it via the inherited setter **after** `.build()`, not inside the builder
  chain — `entity.setTenantId(TenantContext.getCurrentTenantId());` right before `.save(...)`.
  `RoleAssignment` (doesn't extend `BaseEntity` at all — "its own shape", per its existing javadoc)
  gets `tenantId` as a normal direct field instead, so its own `@Builder` **does** expose
  `.tenantId(...)` — don't copy the setter-after-build pattern there, it isn't needed.
- **Registration (`POST /api/v1/register`, public/unauthenticated — the only such endpoint in this
  entire system) is open self-service signup**, not gated behind a platform-owner approval step —
  submitting college + admin details creates the `Tenant` and that tenant's own self-attributed
  SUPER_ADMIN in one transaction and logs the new admin straight in (issues real access/refresh
  tokens in the response, matching `AuthService.login()`'s shape). This is a deliberate, explicit
  reversal of this codebase's otherwise-universal "no self-registration anywhere" posture (see last
  session's `BootstrapAdminRunner` work) — confirmed with the user rather than assumed, given how
  large a security-posture change it is.
- **`BootstrapAdminRunner` (air-gap/on-prem path) and `POST /api/v1/register` (cloud/SaaS path)
  are two entry points into the same underlying mechanism**, matching §5.2's "single-tenant
  deployments use the same schema with a single fixed tenant row — no code changes between modes."
  `UserManagementService.bootstrapTenantAndSuperAdminIfMissing()` now also creates a "default"
  `Tenant` (from `erp.bootstrap.admin.tenant.*` / `BOOTSTRAP_TENANT_*` env vars) before creating its
  SUPER_ADMIN, idempotent the same way as before (a no-op once *any* active SUPER_ADMIN exists
  anywhere, tenant-agnostic). A pure-cloud deployment that only ever expects colleges to
  self-register should set `BOOTSTRAP_ADMIN_ENABLED=false` — left `true`, it would otherwise create
  one unwanted extra "Default College" tenant the very first time the app ever starts, before any
  real signup happens.
- **Tenant `code`/department `code`/course `code`/subject `code`/academic-year `label`/student
  `enrollment_number` uniqueness moved from globally-unique to `(tenant_id, x)`-unique** (V017 drops
  and recreates each index) — two colleges must each be able to have their own "CS" department code,
  their own "2026-2027" academic year, etc. Verified live: registered two tenants, created a
  department with the identical code under both, both succeeded. `users.username`/`users.email`
  deliberately did **not** move to per-tenant uniqueness — see the login-lookup reasoning above.
  FK-anchored unique constraints that don't stand alone (e.g. `batches`' `UNIQUE(course_id,
  academic_year_id)`) needed no change — a `course_id` can only ever belong to one tenant already.
- **`Tenant`'s own field list ("college details") is this session's best-effort guess** — name,
  code, affiliated university, accreditation, established year, address/city/state/pincode/country,
  contact email/phone, website, logo file id (unwired, same `module.storage` situation as every
  other `*_file_id` column in this codebase), timezone, and a minimal `TenantStatus{ACTIVE,
  SUSPENDED}` — no TRIAL/billing-plan states, since nothing in scope asked for billing.
- **No frontend changes** — the frontend doesn't exist yet (task #17). Whoever builds it will need a
  "Register your college" screen hitting this endpoint and to stop assuming a single fixed
  institution anywhere in its design.

Architecture deviation worth knowing about: `security/rbac/GrantResolver` needs `PermissionGrant`
data, which per the docs' own file tree lives in `module/auth/domain` — a feature module the
security *kernel* isn't supposed to depend on (§3.1 module boundary rules). Rather than have the
kernel import a module's repository, `security/rbac/PermissionGrantSource` is an interface the
kernel defines and `module/auth/service/PermissionGrantSourceImpl` implements (`@Primary`, backed
by `PermissionGrantRepository`) — standard dependency inversion, keeps the module graph a real DAG.
The same pattern is used for `UserDetailsService` (`JwtAuthenticationFilter` depends on Spring
Security's own interface, not on `module.auth` directly). Each seam has a `NoOp*` fallback
(`NoOpPermissionGrantSource`, `NoOpUserDetailsService`) so the kernel alone is still startable
before any feature module registers the real implementation. Follow this same pattern for any
future kernel code that needs a feature module's data.

`module/academic` deviations from the docs, worth knowing about:
- `AcademicYearService`/`TermService` and their controllers (`/api/v1/academic/academic-years`,
  `/api/v1/academic/terms`) exist even though neither is in `08-system-architecture.md`'s file
  tree — batches and subject offerings FK to `academic_years`/`terms`, so something has to be able
  to create them. Kept minimal, same conventions as `DepartmentService`.
- **"Timetable" has no day/time-slot data anywhere in `04-database-schema.md`** — `faculty_subject_
  mappings` only says which faculty teaches which subject-offering to which section, not when.
  `TimetableService`/`TimetableEntryDto` expose exactly that (an assignment list), and
  `ConflictDetectionService` only enforces the one conflict the data supports — at most one active
  primary faculty per (offering, section). Don't extend either to claim to do real schedule-clash
  detection without first adding actual time-slot columns via a migration.
- `BatchService.promoteTerm()` assumes 2 terms/year (`TERMS_PER_YEAR`) to compute "final term" —
  the docs never state terms-per-year explicitly, only a `"Semester I"` example name. Revisit if
  that assumption turns out wrong.
- `05-api-documentation.md` §3 only documents Departments/Courses/Batches endpoints for this
  module (Sections/Subjects/Timetable aren't in its TOC at all) — `SubjectController`/
  `TimetableController`/nested section endpoints follow the same conventions (§1) by analogy, not
  from a documented spec. Same situation will likely recur in later modules (e.g. Library only
  documents Catalog+Circulation, not Reservation/Fine) — apply §1's conventions consistently rather
  than blocking on a missing endpoint list.
- DTOs are mapped by hand (`XDto.from(entity)` static factories), not MapStruct as
  `08-system-architecture.md` §2.1 lists in the tech stack — avoids adding an annotation-processor
  dependency before there's enough mapping volume to justify it. Revisit if/when it does.

`module/student` deviations from the docs:
- **`student_documents` has no DDL in `04-database-schema.md` at all** (§7 stops at 7.3
  `guardians`), even though `08-system-architecture.md`'s file tree and its own §12 V004
  description both name `StudentDocument`/`StudentDocumentRepository`/`DocumentVerificationService`.
  Schema in `V004` (`document_type`, `file_id`, `verification_status`, `verified_by`/`verified_at`)
  is inferred from the file-storage design (§8 — `fileId` is a string handle, not a real FK) and
  the verification workflow the file tree implies. Revisit if a real schema surfaces.
- Consequently `StudentDocumentController`'s "upload" endpoint (`POST /students/{id}/documents`)
  only *records* a `fileId` a prior storage call returned — it's not a multipart handler, because
  `module/storage` (task on the list, not started) doesn't exist yet to produce that `fileId`. Wire
  the frontend's actual upload flow through `FileStorageService.store()` once that module exists,
  not through this endpoint directly.
- No `AlumniService`/alumni endpoints — Alumni Management is explicitly Phase 2 scope
  (`01-project-vision.md` §9). `StudentService.graduate()` only flips `status`/`graduation_date`.
- Guardian endpoints (`GET`/`POST /students/{id}/guardians`) live on `StudentController` — no
  dedicated `GuardianController` in the file tree, and `05-api-documentation.md` §4 has no guardian
  endpoints at all. Same "extend by analogy" situation as academic's undocumented endpoints above.
- `EnrollmentService.createInitialEnrollment()`/`transferSection()` need actual `Batch`/`Section`/
  `Term` JPA entities (not DTOs) to set `@ManyToOne` associations on `Enrollment` — so
  `BatchService`/`SectionService` each gained a `get*Entity(id)` accessor alongside their existing
  DTO-returning `getById()`. This isn't a new pattern: it's the same thing
  `07-coding-standards.md` §4.4's own "correct" example does (`studentService.getById(studentId)`
  returns a `Student` entity there, not a DTO) — cross-module calls go through the other module's
  *service*, and that service is free to expose entities as well as DTOs. Follow this same
  `get*Entity()` naming for any future module that needs to attach to another module's entity.

`module/attendance` deviations from the docs:
- `attendance_policies.course_id` is `UNIQUE` but has **no `REFERENCES`** in the doc's own DDL —
  every other `course_id` column in the schema does have one. Added the FK in `V005` anyway
  (`courses` already exists as of `V003`); near-certainly a doc omission, not intentional.
- `03-srs.md` [ATT-004]'s formula ("present count / total sessions count") doesn't say how
  `LATE`/`EXCUSED`/`HOLIDAY` factor in. `AttendanceRulesEngine` interprets it as: `HOLIDAY` never
  counts in the denominator; only `PRESENT` counts as present; `LATE`/`EXCUSED` count toward the
  denominator but neither bucket (matching the 4 fields the API doc's summary response actually
  has). Same reasoning for [ATT-007] consecutive-absence streaks: scoped per (student, offering) —
  the doc doesn't say whether it's per-subject or across all of a student's subjects, and per-
  subject is the only reading that makes "N consecutive classes missed" mean anything concrete.
- [ATT-007]'s parent-alert notification is **not dispatched** — `module.notification` doesn't
  exist. `AttendanceCaptureService.checkConsecutiveAbsenceAlerts()` detects the threshold breach
  correctly and logs it at WARN as a stand-in; wire it to `NotificationService.send()` once that
  kernel piece is built, don't fabricate a fake dispatch now.
- Extended the `get*Entity()` cross-module pattern further: `SubjectService.getOfferingEntity()`,
  `TermService.getTermEntity()`, `StudentService.getStudentEntities(Collection<UUID>)` (batch
  variant — validates a whole session's worth of student ids in one query, throws naming the first
  missing one), and `StudentService.getStudentEntityByUserId()` (resolves "my own student profile"
  for self-service endpoints like condonation requests, called with the JWT principal's user id,
  never a client-supplied `studentId`).
- Only one `AttendanceController` exists (matching the file tree) — condonation endpoints live
  there too, not a separate controller.

`module/examination` deviations from the docs — the biggest set so far:
- **`ExamSchedule`, `SeatingAllocation`, `InvigilatorAllocation`, `QuestionPaper` are not built at
  all.** The file tree names all four as domain classes, but none has any DDL anywhere in
  `04-database-schema.md` (unlike `student_documents`, which at least got a passing mention — these
  four have nothing). Rather than invent four more undocumented schemas, only the five
  fully-specified tables (`exams`, `hall_tickets`, `marks_entries`, `result_records`,
  `revaluation_requests`) exist. `HallTicketDto` has no seating-info field as a direct consequence
  — the API doc's own example response mentions "seating info" but there's no table backing it.
- `exams.status` and `revaluation_requests.status` have no value list in the doc's column comments
  (unlike `exam_type`, `marks_entries.status`, `result_records.result_status`, which do). Inferred
  `ExamStatus` = SCHEDULED/ONGOING/COMPLETED/CANCELLED and `RevaluationStatus` =
  PENDING/COMPLETED/REJECTED (matching the columns that actually exist — no separate "assigned to
  re-evaluator" tracking column, so no ASSIGNED state). Neither is enforced by a DB CHECK
  constraint, consistent with how every other status column in this schema works.
- **[EXAM-005]'s "institution-configured grading scale" doesn't exist as a table anywhere.**
  `GradingScale` hardcodes the standard 10-point scale (O/A+/A/B+/B/C/P/F) used across Indian
  universities — verified against the API doc's own worked example (90% → "O", 10.0). Every caller
  goes through this one class, so swapping in a real config-driven scale later is a one-file change.
- **[EXAM-006]'s PDF grade card is not generated.** No PDF library in `pom.xml`, and `module.storage`
  (needed to persist the file) doesn't exist. `GradeCardService`/`ResultDto` provide the exact data
  a grade card needs (matches the API doc's result-response shape exactly); add rendering later
  without touching the data layer.
- **Result computation aggregates per (student, term), not per exam.** `result_records` is
  `UNIQUE(student_id, term_id)` but `marks_entries` is `UNIQUE(exam_id, offering_id, student_id)` —
  a term can have multiple exams (INTERNAL_1, INTERNAL_2, END_TERM). Publishing any one exam
  recomputes the whole term's `ResultRecord` from ALL currently-published entries for that student
  in that term, so results accumulate correctly as exams publish incrementally. The docs don't
  spell this multi-exam aggregation out, but it's the only reading consistent with both UNIQUE
  constraints.
- Revaluation "changing a mark" (03-srs.md [EXAM-007]) is interpreted as replacing
  `external_marks` specifically, not `internal_marks` or `total_marks` directly — revaluation
  conventionally applies to the externally-set final exam paper, not a faculty's continuous
  internal assessment. `RevaluationService` re-runs the exact same `ResultProcessingService.gradeEntry()`
  logic used at publish time so a re-graded entry is computed identically to a first-time one.
- `05-api-documentation.md` §6's auth note "FACULTY (assigned to this offering)" on marks entry
  isn't expressible as a role alone — enforced via `@PreAuthorize` SpEL calling
  `@timetableService.isFacultyAssignedToOffering(...)`, the same bean-in-SpEL pattern as
  `@studentSecurityService.isOwnProfile`. `EXAM_CELL`/`SUPER_ADMIN` bypass the check entirely (they
  aren't in `faculty_subject_mappings` for any offering).

**`module/finance` required a kernel change**, not just a module-local deviation: the schema has
*two* functionally-identical grant tables — module.auth's `permission_grants` (§5.3) and this
module's `accountant_permission_grants` (§10.8, `03-srs.md` [FIN-008]) — same shape (grantee,
scope, threshold, revocation), same purpose (Layer-2 scoped RBAC). `security/rbac/GrantResolver`
used to take exactly one `PermissionGrantSource` (with `module.auth`'s implementation marked
`@Primary` over a `NoOpPermissionGrantSource` fallback); it now takes `List<PermissionGrantSource>`
and aggregates every registered source, so a user's active grants can come from either table.
`NoOpPermissionGrantSource` was deleted (Spring injects an empty list when zero sources exist, so
no placeholder bean is needed) and `@Primary` was dropped from `module.auth`'s
`PermissionGrantSourceImpl` (nothing "wins" anymore — both are consulted together). Follow this
list-of-sources pattern, not a single-`@Primary`-source pattern, for any future kernel seam a
second module might also need to contribute to.

`module/finance` deviations from the docs:
- **`Scholarship`, `Refund`, `Budget`, `Payroll`, `BankReconciliation`, `FinancialReport` are not
  built at all.** Same situation as examination's four undocumented entities: the file tree names
  controllers/services for all six, but none of the underlying tables (`scholarships`,
  `refund_requests`, `budgets`, `payslips`, ...) has any DDL anywhere in `04-database-schema.md`.
  [FIN-006] (scholarships/waivers) and [FIN-007] (refunds) are consequently unimplemented too.
- **[FIN-010]'s three-way match (PO → GRN → vendor invoice) isn't implemented** — no
  goods-received-note table exists either. `ProcurementService` covers vendor + PO create/approve
  only, not receiving.
- Added a `(student_id, batch_id, term_number)` UNIQUE constraint on `fee_invoices` that isn't in
  the doc's own DDL (only `invoice_number` is documented as unique) — defense-in-depth for
  [FIN-002]'s duplicate-invoice rule, which the doc otherwise relies on app-level checking alone for.
- **Fee structure resolution**: when both a course-wide (`batch_id IS NULL`) and a batch-specific
  row exist for the same fee head/term, `FeeStructureService.resolveApplicable()` treats the
  batch-specific row as an *override*, not an additament — summing both would double-charge that
  fee head. [FIN-001]'s "different amounts per batch" phrasing is the only reading that makes sense
  of this, but the doc never states the override-vs-add rule explicitly.
- **Late fee ([FIN-005])** is computed once per invoice, on the first payment attempt made after
  the due date (guarded by `late_fee` still being zero) — not recalculated on every subsequent
  partial payment. The rate used is the max `late_fee_per_day` across that invoice's applicable fee
  structures, mirroring the same override-resolution `resolveApplicable()` already does for amounts.
- **Manual ledger entries are genuinely single-sided**, not auto-paired like fee collection — the
  documented request shape (05-api-documentation.md §7 POST /finance/ledger/manual-entry) is one
  account head + one entry type per call, even though [FIN-004] calls for "paired entries" in
  general. A balanced correction is the Head Accountant's own responsibility (two calls, shared
  `reference_id`); `LedgerService` doesn't invent a second leg that isn't in the request.
- **`ACCOUNTS_RECEIVABLE` and `FEE_INCOME` account heads are lazily auto-created** on first fee
  collection (`AccountHeadService.getOrCreateSystemHead`) rather than requiring seed data — V014
  (seed data) doesn't exist yet and fee_heads has no FK to account_heads in the schema at all, so
  there was no documented way to know which account head backs which fee head. Revisit if seed
  data or an explicit fee_head→account_head mapping is ever specified.
- **Financial year close** requires two distinct users per [FIN-009]'s "dual approval" — enforced
  as "the same user cannot call this twice," not "one call must carry role HEAD_ACCOUNTANT and the
  other ADMIN specifically." The doc doesn't state whether one person holding both roles is allowed
  to self-approve; this reading is the more conservative one and matches the API doc's own
  two-call description exactly.
- **Fine-grained accountant-grant scoping (department/fee-head/amount) isn't enforced yet** inside
  `FeeCollectionService` — the API doc's "Scope: accountant may be restricted to specific
  departments/fee-heads" note on the collect-payment endpoint isn't checked beyond the coarse
  `@PreAuthorize` role gate. The `GrantResolver`/`PermissionScope` infrastructure to do this exists
  (see the kernel-change note above) but isn't yet called from `FeeCollectionService` per-invoice.
  Worth doing before this module is considered complete.
- `FEE_COLLECTION_CLERK` added to `UserRole` — only surfaces once, in this module's API doc auth
  note, not in any persona or shell-layout list elsewhere.

`module/library` deviations from the docs:
- **`LibraryMember`, `EResource`, `LibraryAcquisition` are not built**, nor is `StockVerificationService`
  ([LIB-009]'s stock-take mode) — same undocumented-entity situation as examination/finance (file
  tree names them, `04-database-schema.md` has no DDL for any of them). Borrowers don't need a
  `library_members` table anyway: `library_custody_log.borrower_user_id` already references
  `users(id)` directly, scoped by `borrower_type` against `loan_policies` — nothing is lost by
  skipping it.
- **Barcode images are generated on demand, never persisted.** `book_copies.barcode_image_file_id`
  implies storing a file via `module.storage` (doesn't exist yet), but a Code 128 barcode is fully
  reconstructible from its content string alone — `GET .../barcode-image` calls `BarcodeService`
  fresh on every request instead. No `BarcodeSheetDto`/printable label sheets either — that's a PDF
  concern, same reasoning as the examination module's grade card.
- **`loan_policies.renewals_allowed` is not enforced.** `library_custody_log` has no renewal-count
  column anywhere in the schema, and a renewal extends `due_at` on the existing row rather than
  inserting a new one (matching the API doc's own response shape) — there's nowhere documented to
  persist how many times a given loan has already been renewed. Flagged in `CirculationService`'s
  own javadoc too.
- **Accession numbers are `count()+1`-based, not a real DB sequence** — unlike invoice/receipt
  numbers elsewhere, which use random suffixes specifically to dodge this race condition. Acceptable
  at library scale (low concurrent-write volume on copy registration) but worth revisiting if it
  ever isn't.
- **Fine-waiver authority reuses kernel RBAC directly, not `@PreAuthorize`.** [LIB-006]'s
  amount-scoped threshold for Library Assistants can't be expressed through the generic
  `hasPermission(...)` SpEL form (no way to carry *this specific fine's amount* into the
  evaluation), so `FineService.assertWaiverAuthority()` re-runs the same Layer-1 (`RoleTemplate`)
  → Layer-2 (`GrantResolver` with a `PermissionScope` built from the fine's balance) logic
  `PermissionEvaluator` uses, directly in the service. Added `CHIEF_LIBRARIAN` → `LIBRARY.FINE_WAIVER`
  to `RoleTemplate` (unconditional, matching "the Chief Librarian may waive a fine" — no scoping
  mentioned for that role, unlike Library Assistants). Follow this same "re-run the two layers with
  a real scope" pattern for any future case where a permission needs a resource-specific amount/
  scope value a generic `hasPermission(...)` call can't supply.
- Reservation pickup notification ([LIB-007]) isn't dispatched, only detected — see the
  `module.notification` row in the Build progress table above.

`module/asset` deviations from the docs:
- **`AssetTag`, `MaintenanceSchedule`, `AnnualMaintenanceContract`, `AssetDisposal`,
  `DepreciationRecord`, `ConsumableItem`, `RoomBooking` are not built** — same undocumented-entity
  situation as examination/finance/library (08-system-architecture.md's file tree names them,
  `04-database-schema.md` §12 has no DDL for any of them). `asset_tag` is just a `VARCHAR` column
  on `assets`, not a separate table, so no `AssetTag` entity is needed either way. Disposal
  ([AST-004]) is a plain `status = DISPOSED` transition on `assets`, not a separate
  `AssetDisposal` record.
- **Depreciation ([AST-005]) is computed on demand, never persisted.** There is no
  `depreciation_records` (or similarly named) table anywhere in the schema to hold an annual
  snapshot into, so `DepreciationService.calculateCurrentValue()` returns straight-line book value
  as of *today* from `purchase_cost` / `purchase_date` / the category's `depreciation_rate_pct`,
  not a stored yearly schedule. `GET /assets/{id}/depreciation` isn't in
  05-api-documentation.md's TOC either — added for symmetry with the other on-demand
  calculators (`GradeCardService`, etc.).
- **[AST-004]'s "Asset Manager to initiate + Admin to confirm (if above value threshold)" two-party
  disposal workflow is simplified to a single call with a role-based gate**, not a literal
  two-step handshake. `assets.status`'s documented enum (`ACTIVE | UNDER_MAINTENANCE | DISPOSED |
  LOST | TRANSFERRED`) has no intermediate "pending disposal" value and there's no disposal-request
  table to hold an in-flight initiation between two calls. `AssetTransferService.dispose()` instead:
  below `erp.asset.disposal-value-threshold` (default 50,000, matching the shape of
  `erp.finance.segregation-of-duties-threshold`) an `ASSET_MANAGER` may dispose directly
  (05-api-documentation.md §9's controller-level auth is `SUPER_ADMIN`/`ASSET_MANAGER`); at or
  above it the caller must specifically hold `SUPER_ADMIN` — reading `hasRole('SUPER_ADMIN')`
  directly off `SecurityContextHolder` inside the service (same "re-run a check `@PreAuthorize`
  can't express" pattern as library's fine-waiver, see above). Revisit if a disposal-request table
  is ever added to the schema.
- **Two FK omissions fixed in `V009`**, same pattern as `attendance_policies.course_id` in `V005`:
  `assets.department_id` and `asset_custody_log.to_department_id` are both `NOT NULL` in
  `04-database-schema.md` §12 but lack a `REFERENCES departments(id)` clause there even though
  `departments` already exists (`V003`).
- **`AssetCustodyLog` is fully immutable at the Java level too** — unlike `LibraryCustodyLog`
  (partially immutable, `recordReturn()`/`extendDueDate()` still allowed), it has no mutators at
  all, mirroring `trg_acl_no_update`/`trg_acl_no_delete` (both reuse `V002`'s shared
  `fn_reject_modification()`) which reject any `UPDATE`/`DELETE` outright.
- Added `getVendorEntity(UUID)` to `module.finance`'s `ProcurementService` — `Asset.vendor` needs
  the managed `Vendor` entity for its `@ManyToOne`, following the same cross-module accessor
  pattern as `DepartmentService.getDepartmentEntity`/`BatchService.getBatchEntity`.
- The module's own barcode wrapper is named `AssetBarcodeService`, not `BarcodeService` — a plain
  `BarcodeService` bean name collided with `module.library`'s existing bean of the same
  simple-class-name at context-scan time (`ConflictingBeanDefinitionException`) even though the two
  live in different packages. Both are stateless ZXing Code 128 wrappers, intentionally duplicated
  per module rather than shared, to keep the module-boundary rule intact.

`notification`/`scheduler` kernel deviations from the docs:
- **EmailChannel/SmsChannel are stubs — no SMTP client or SMS gateway is wired up.** Neither
  `pom.xml`'s dependencies nor any of the eight planning docs specify SMTP host/credentials or an
  SMS provider, so there's nothing concrete to integrate against. Both beans always exist; their
  `isEnabled()` reads `erp.notification.email.enabled`/`erp.notification.sms.enabled` (both
  `false` by default, matching ADR-001 air-gap-first), and `send()` logs the outbound attempt at
  INFO rather than silently dropping it — the same "detected but only logged, documented as a
  stand-in" pattern used everywhere else in this codebase for an undocumented integration point.
  Add a real `JavaMailSender`/gateway client before turning either flag on outside local testing.
- **Only 4 of the 5 [SCHED-004] cron jobs are wired up.** `AttendanceShortfallScanJob`,
  `FeeDueDateReminderJob`, `LibraryOverdueScanJob`, and `PermissionGrantExpiryJob` all have real
  `JobHandler` implementations reusing existing module query methods (a few new ones added:
  `SubjectOfferingRepository.findByDeletedFalse()`, `FeeInvoiceRepository
  .findByStatusInAndDueDateBetweenAndDeletedFalse()`, `LibraryCustodyLogRepository
  .findAllOverdue()`, `PermissionGrantRepository.findExpiringBetween()`).
  `MaintenanceRenewalAlertJob` (AMC renewal, Mondays 09:00) is **not implemented or enqueued** —
  no `AnnualMaintenanceContract` table exists anywhere in `04-database-schema.md` (already flagged
  as deferred in the asset module's own deviations above), so there's no data to scan.
  `BackupVerificationJob` (present in 08-system-architecture.md's file tree, not in 03-srs.md
  [SCHED-004]'s list) is also **not implemented** — none of the eight docs specify a backup
  mechanism to verify. `ScheduledTasks` only enqueues the four it can actually run; it doesn't
  enqueue dead jobs with no registered handler.
- **[ATT-007]/[LIB-007]'s previously-WARN-only alerts are now real notifications.**
  `AttendanceCaptureService.checkConsecutiveAbsenceAlerts()` and
  `ReservationService.notifyNextInQueue()` both now call `NotificationService.send()` instead of
  just logging. Recipient resolution added `Guardian.userId` to `GuardianDto` (was missing —
  Guardian already had the column, the DTO just didn't expose it) so a guardian with a linked user
  account gets notified in preference to the student; falls back to the student's own account
  (`Student.userId`) if no guardian has one, and logs a WARN only in the (should-be-rare) case
  where neither exists. `AttendanceShortfallScanJob`'s nightly sweep uses the identical
  resolve-guardian-or-fall-back-to-student logic for the same reason.
- **`notification`/`scheduler` are platform-kernel packages** (`com.college.erp.notification`,
  `com.college.erp.scheduler`), not `module/` packages — matching 06-design-document.md's
  dependency diagram, which places them alongside `auth`/`audit`/`storage` as things every module
  depends on. This means the flow direction is unusual in one place: `scheduler.jobs.*` (kernel)
  calls into `module.X.service.Y` (business modules) to gather data to notify about — the reverse
  of the normal "modules depend on kernel" direction — because that's exactly what
  08-system-architecture.md's file tree names these job classes to do. No new cross-module rule is
  needed beyond the existing "call the other side's service, not its repository" — though when
  this was first written, three of the four job handlers actually violated that rule by injecting
  a repository directly; `ModuleBoundaryTest` (task #18) caught it and it was fixed then. See that
  task's CLAUDE.md notes for exactly what changed.
- **`background_jobs.payload` maps straight to Postgres `jsonb` via Hibernate 6's
  `@JdbcTypeCode(SqlTypes.JSON)` on a plain `String` field** — no extra JSON-mapping library
  (`hypersistence-utils` etc.) needed, since Boot 3.3.5 ships Hibernate 6.
- **No `NotificationTemplate` rows are seeded.** Every `NotificationService.send()` call so far
  uses a raw title/body (matching `AuditLogWriter`'s raw-description convention) rather than a
  `templateKey`; template rendering (`TemplateRenderer`, `{{variable}}` substitution) is built and
  tested but not yet exercised by real callers — wire it up once actual template content is
  specified somewhere.

`module/hr` deviations from the docs:
- **`AppraisalCycle`/`AppraisalForm`/`AppraisalService`/`AppraisalController` are not built** — same
  undocumented-entity situation as examination/finance/library/asset: named in
  08-system-architecture.md's file tree, zero DDL in `04-database-schema.md` §13, zero `[HR-xxx]`
  requirement in 03-srs.md §3.10, zero endpoint in 05-api-documentation.md §10. `PayrollService`
  from the same file tree is similarly undocumented and deferred (staff_profiles has bank/PAN/PF
  columns for *future* payroll use, per its own comment, but no payroll table exists to compute
  or record an actual payroll run against).
- **[HR-004]'s "HOD → HR Manager" two-stage approval chain is modeled as single-stage**, same
  schema-constrained simplification as asset disposal ([AST-004]): `leave_requests` has exactly
  one `reviewed_by`/`reviewed_at`/`review_remarks` slot (not two), and
  05-api-documentation.md §10 exposes exactly one approve/reject endpoint open to any of
  `SUPER_ADMIN`/`HR_MANAGER`/`HOD`/`ADMIN` — whichever of those roles acts first settles it. See
  `LeaveRequest`'s javadoc.
- **`LeaveBalance` does not extend `BaseEntity`** — `leave_balances` is the one HR table missing
  `created_by`/`updated_by` columns (see the DDL comment in V011); same "entity's column set has
  to match what the table actually has" reasoning as `AssetCustodyLog`/`LibraryCustodyLog`.
- **Balance initialization ([HR-002]: "initialized at the start of each academic year") is a
  manually-triggered, idempotent bulk operation** (`POST /hr/leave/balances/initialize?year=`),
  not an automatic cron job — nothing in 03-srs.md [SCHED-004] lists a trigger time for it, unlike
  the four jobs the scheduler kernel does wire up.
- **`StaffProfile.userId`, `LeaveBalance.userId`, `LeaveRequest.userId`/`reviewedBy` are plain
  `UUID` columns, not managed `User` entity relations** — consistent with how every other module
  references a user peripherally (`LibraryCustodyLog.borrowerUserId`, `Asset.assignedToUserId`,
  `MaintenanceTicket.reportedBy`, ...); `module.hr` never needs to load the full `User` entity.

`module/hostel` + `module/transport` deviations from the docs — **read this before touching
either module**:
- **These are the only two modules built so far with no 03-srs.md functional requirements and no
  05-api-documentation.md endpoint section at all** — not even the usual "the file tree names more
  than the schema backs" gap seen everywhere else. `04-database-schema.md` §14 has the full DDL
  (7 tables, no append-only/immutable ones), but 03-srs.md has zero `[HOSTEL-xxx]`/`[TRANS-xxx]`
  requirement codes and 05-api-documentation.md's table of contents skips straight from §10 "HR &
  Leave" to §11 "Communication". `01-project-vision.md` line 308 explains why:
  "Phase 2 | Hostel, Transport, Admissions, Placement | 12 weeks" — these are Phase 2 modules
  whose schema was drafted ahead of their own SRS/API pass, unlike task #15
  (admissions/placement) which is *also* Phase 2 and was correctly left for later. Built here
  anyway since the schema fully supports it and the task list already sequenced it before #15 —
  but every business rule below is inferred from the DDL and ordinary domain sense, not a cited
  requirement. Treat this module as lower-confidence than any other when reconciling it against a
  future SRS/API pass for Phase 2.
- **`RoomAllocationStatus` (`ACTIVE`/`VACATED`) and `OutpassStatus`
  (`PENDING`/`APPROVED`/`REJECTED`/`CANCELLED`) are inferred enums** — the DDL comments for
  `room_allocations.status` and `outpass_requests.status` don't enumerate values (unlike e.g.
  `assets.status`, which does). `OutpassStatus` was deliberately shaped to match
  `LeaveRequestStatus`'s four values since outpass approval is the same
  apply→approve/reject/cancel lifecycle as leave requests.
- **Room capacity is enforced** (`RoomAllocationService.allocate()` rejects once
  active allocations reach `room.capacity`) — a sensible default, not a cited rule.
  **`hostel_blocks.capacity` is not cross-checked against its rooms' capacities** — nothing in the
  schema states that relationship must hold, so it's treated as an independent, informational field.
- **A student may only have one active route mapping at a time**
  (`StudentRouteMappingService.assign()` rejects a second one) — again ordinary domain sense
  (a student takes one bus route), not a documented constraint.
- **No dedicated `HostelController`/`TransportController` request/response shapes are documented**
  — the endpoint paths, field names, and auth role combinations
  (`SUPER_ADMIN`/`HOSTEL_WARDEN`/`STUDENT` for hostel, `SUPER_ADMIN`/`TRANSPORT_MANAGER` for
  transport) were chosen for consistency with the rest of the API, not transcribed from a spec.

`module/communication` deviations from the docs:
- **[COMM-002] says helpdesk tickets are "assigned to the relevant department", but
  `helpdesk_tickets` has no `department_id` column** anywhere in `04-database-schema.md` §15.2 —
  only `assigned_to` (a direct user reference). Followed the schema: `HelpdeskService.assign()`
  assigns to a specific staff user, not a department queue.
- **`TicketPriority`'s value set (`LOW`/`MEDIUM`/`HIGH`/`CRITICAL`) is inferred** — the DDL comment
  for `helpdesk_tickets.priority` doesn't enumerate values (unlike `category`, which does);
  matched to `module.asset`'s `maintenance_tickets` shape and the `"priority": "HIGH"` example in
  05-api-documentation.md §11.
- **`GET /notices`'s "relevant to the current user" isn't resolved from the caller's identity.**
  There's no existing accessor for "this user's current department/batch" (a student's batch
  lives behind `Enrollment`, not on `StudentDto`; building that accessor was out of scope for this
  module) — `NoticeService.getRelevant()` instead takes `departmentId`/`batchId` as optional
  query parameters the caller supplies explicitly. `ALL` and `ROLE`-targeted notices are always
  included regardless. See `NoticeService`'s javadoc.
- **`ROLE`-targeted notices are accepted but not filterable by role.** `notices.target_id` is
  `UUID`-typed and can't hold a role name, so every `ROLE`-type notice is returned to every
  authenticated caller in `getRelevant()` rather than being scoped to the viewer's actual role —
  same reasoning as the `target_id` column comment only mentioning `department_id`/`batch_id`,
  never a role.
- **Only `POST /helpdesk` and `GET /helpdesk/my-tickets` are in 05-api-documentation.md §11's
  TOC** — `assign`/`start-progress`/`resolve`/`close`/comment endpoints follow directly from
  [COMM-002]'s described `OPEN → ASSIGNED → IN_PROGRESS → RESOLVED → CLOSED` lifecycle and "any
  authenticated user may raise... comments may be added by the ticket owner or the assigned
  staff" text, same "SRS describes more than the API doc's TOC enumerates" situation as
  library's `FineController`.

`module/admin` deviations from the docs:
- **`UserManagementService` operates directly on `module.auth`'s `User`/`RoleAssignment`
  repositories, not through a `module.auth` service** — a deliberate exception to the usual
  "cross-module access goes through the other module's service" rule
  (07-coding-standards.md §4.4). 08-system-architecture.md's own file tree places
  `UserManagementService`/`UserManagementController` under `module.admin` operating on auth's
  entities directly, and no fuller-lifecycle `UserService` exists in `module.auth` to route
  through (`AuthService` only covers login/refresh/logout/change-password). See the class javadoc.
- **`NoticeController` lives under `com.college.erp.notification`, not `module.admin`, even
  though 08-system-architecture.md line 1107 lists a `NotificationController` under
  `module.admin/controller`.** The docs are internally inconsistent here: 06-design-document.md
  §6's dependency diagram and 08-system-architecture.md's own earlier file tree (§ line ~562-580)
  both place notification under the shared platform kernel, alongside `audit`/`security`/
  `storage`, which is where it was already built (task #10, before `module/admin` existed) and
  is the architecturally consistent placement — every module depends on notification, so it
  belongs in the kernel, not inside one specific business module. Not moved.
- **Reports (02-prd.md §9.1/§9.2) are not built at all**, not even as structured-data-without-
  rendering the way `GradeCardService` handles PDF grade cards — Reports needs a PDF/Excel export
  library (`pom.xml` has neither) and aggregates data across nearly every module (fee defaulters,
  attendance summaries, financial statements, library circulation, asset inventory, leave
  utilization...), which is out of proportion for a single pass. `ReportBuilderService`/
  `ReportController` from 08-system-architecture.md's file tree are unimplemented.
- **`SystemHealthController`** (also named only in 08-system-architecture.md's file tree) **is not
  built** — zero backing text in 03-srs.md, 02-prd.md, or 05-api-documentation.md.
- **`UserManagementService.create()` sets `mustChangePassword = true`** on every admin-created
  account — not explicitly required by 05-api-documentation.md §13, but a reasonable default for
  an admin-issued temporary password (`"password": "TempPass@123"` in the endpoint's own example
  reads like a temp password), consistent with `User.mustChangePassword`'s existing purpose.
- **There is no public self-registration endpoint anywhere in this system — by design** (a college
  ERP's accounts are provisioned by administration, not self-signed-up; none of the eight docs
  documents a `/register` endpoint either). That created a real bootstrap deadlock a fresh database
  had no way out of: `POST /admin/users` — the only user-creation path that exists — itself
  requires an already-authenticated `SUPER_ADMIN`/`ADMIN` caller, so a brand-new database could
  never create its first account through the API at all. Fixed with `BootstrapAdminRunner`
  (`com.college.erp.config`, kernel) + `UserManagementService.bootstrapSuperAdminIfMissing()`: an
  `ApplicationRunner` that runs once per startup and idempotently seeds exactly one `SUPER_ADMIN`
  account if no active `SUPER_ADMIN` role assignment exists anywhere yet (checked via the new
  `RoleAssignmentRepository.existsByRoleAndActiveTrue`) — a genuine no-op, safe on every restart,
  not just the first. Credentials come from `erp.bootstrap.admin.*`
  (`BOOTSTRAP_ADMIN_USERNAME`/`_EMAIL`/`_FULL_NAME`/`_PASSWORD`/`_ENABLED` env vars, see
  `ErpProperties.Bootstrap.Admin`'s javadoc), default username/password `superadmin` /
  `ChangeMe@123` — **change these before any non-local deployment**, they're documented in
  `application.yaml` and the Postman collection's Login example. The created role assignment is
  self-attributed (`assignedBy` = its own id) since `role_assignments.assigned_by` is `NOT NULL`
  with no "system" sentinel anywhere in the schema — there is, by definition, no prior actor to
  attribute a system's very first grant to. This is a kernel component depending on a module
  *service* (`UserManagementService`), the same sanctioned direction `scheduler.jobs.*` already
  uses to call into module services — `ModuleBoundaryTest` doesn't even evaluate it, since
  `com.college.erp.config` isn't a `module.*` package.

`storage` kernel deviations from the docs:
- **`MinioFileStorageService`/`S3FileStorageService` are not implemented.** Neither the MinIO
  client nor the AWS S3 SDK is a `pom.xml` dependency, and adding one is a bigger scope/dependency
  decision than a doc-driven build pass takes on — same reasoning as `EmailChannel`/`SmsChannel`
  staying stubs rather than integrating a guessed provider. `LocalFileStorageService`
  (`erp.storage.mode=local`, the default — air-gap safe, ADR-001) is the only implementation;
  `FileStorageService` is written so a future `@ConditionalOnProperty`-gated `minio`/`s3`
  implementation slots in without any caller changing.
- **`fileId` is located by scanning each `StorageScope` subdirectory**, not looked up in a
  database. There is no file-metadata table anywhere in `04-database-schema.md` to record which
  scope/extension a given UUID belongs to, and `08-system-architecture.md` states the returned
  `fileId` is just a bare UUID (not a full path) — so `retrieve()`/`delete()`/`getContentType()`
  check for a file named `"{fileId}.*"` in each of the 6 `StorageScope` subdirectories in turn.
  O(number of scopes), not O(files) — fine at this system's scale, but revisit if a
  `stored_files` metadata table is ever added.
- **`FileStorageService` gained a `getContentType(fileId)` method beyond the design doc's
  three-method interface** (`store`/`retrieve`/`delete`) — `FileController` needs a Content-Type
  for the response header and there's no metadata table to source it from either, so
  `LocalFileStorageService` derives it from the stored file's extension via
  `Files.probeContentType()`.
- **No module actually calls `FileStorageService.store()` yet.** Every place the schema has a
  `*_file_id` column (student documents, asset/library barcode+photo images, notice attachments,
  ...) still either generates images on demand (barcodes) or doesn't persist an upload at all.
  Wiring each of those call sites is a follow-up integration task, not part of standing up the
  storage kernel itself.

**`ModuleBoundaryTest` (ArchUnit) surfaced and fixed a real violation while it was being written:**
four `scheduler.jobs.*` handlers (`AttendanceShortfallScanJob`, `FeeDueDateReminderJob`,
`LibraryOverdueScanJob`, `PermissionGrantExpiryJob`) had been injecting another module's
`*Repository` directly instead of going through that module's service — contradicting what this
file's own notification/scheduler notes claimed ("scheduler.jobs already follows [call the
service, not the repository]"). Fixed by adding one accessor method each to
`SubjectService.listAllOfferingEntities()`, `FeeCollectionService.listDueSoonEntities(...)`,
`CirculationService.listAllOverdueEntities()`, and
`PermissionGrantSourceImpl.findExpiringBetween(...)` (that last one already owned
`PermissionGrantRepository` for an unrelated reason — `GrantResolver`'s Layer 2 lookup — so the
new method just reuses it), then pointing each job handler at the service instead. Confirmed via
`ModuleBoundaryTest` that this was the *only* real violation anywhere in the codebase — see that
class's own javadoc and the JDK-26/ArchUnit note above for how that was verified, not just assumed
from a clean run.

`module/admissions` + `module/placement` — **read the "INVENTED schema" note near the top of this
file first.** Every deviation below is really "what this session chose to invent", not "how this
session deviated from a spec that doesn't exist":
- **`V015`/`V016` are entirely hand-designed schemas**, built by inference from
  08-system-architecture.md's file tree (class names only, no fields) plus this project's own
  established conventions (UUID PKs, soft delete, `TIMESTAMPTZ`, `NUMERIC` for money) and, where a
  closer analogy existed, the shape of an already-built module: `applications`'s
  SUBMITTED→...→ADMITTED lifecycle mirrors `module.hr`'s `LeaveRequest`/`module.communication`'s
  `HelpdeskTicket` (single-stage review, no cited two-stage chain); `application_workflow_steps`
  is a fully immutable append-only audit trail reusing `fn_reject_modification()`, the same
  pattern as `asset_custody_log`/`library_custody_log`. `V014` is deliberately skipped (reserved
  for `04-database-schema.md` §19's seed data, which doesn't exist as a migration yet).
- **`ApplicationCategory`/`DocumentVerificationStatus` mirror `module.student`'s
  `StudentCategory`/document-verification shape** rather than inventing an unrelated vocabulary —
  an admissions applicant becomes a student, so reusing the receiving module's categories reduces
  translation friction at conversion time.
- **Merit ranking has no cited tie-break or weighting rule** — `MeritProcessingService.generate()`
  sorts every SHORTLISTED application for a course/year by `qualifying_exam_score` descending
  (nulls last) and assigns sequential ranks. This is the simplest defensible reading given the
  columns that actually exist; there's no reservation-category weighting, sports quota, or other
  real-world merit-list nuance modeled.
- **Seat allotment `batch_id` is nullable** — a course may not yet have next year's `Batch` rows
  created when seats are first offered, so `MeritProcessingService.offerSeat()` accepts a null
  batch and `EnrollmentConversionService.convert()` (which does require a batch) is the point
  where that becomes mandatory, not seat-offer time.
- **`EnrollmentConversionService` does not publish a `StudentEnrolledEvent`.**
  06-design-document.md's cross-module table names exactly that event
  ("admissions.EnrollmentConversionService | StudentEnrolledEvent | academic | Set up subject
  offerings"), but no `ApplicationEventPublisher`/`@EventListener` infrastructure exists anywhere
  else in this codebase either — library's own documented `BookIssuedEvent` was never wired up
  either. Rather than build that infrastructure for the first time inside an already-invented
  module, `EnrollmentConversionService` calls `module.student.service.StudentService.create()`
  directly (an ordinary, sanctioned cross-module *service* call — see `ModuleBoundaryTest`, which
  passes clean on this). Revisit once the event bus pattern is established for real, ideally
  driven by a module that has a documented reason to need it.
- **Section assignment is a required, caller-supplied parameter at conversion time**
  (`ConvertToStudentRequest.sectionId`), not resolved automatically — `module.student`'s
  `StudentService.create()` itself requires a `sectionId`, and admissions has no natural way to
  pick one (that's an academic-office operational step, not an admissions decision).
- **`PlacementAnalyticsService` is scoped per drive, not per academic year** — `PlacementOffer` has
  no direct `academic_year_id`, only reachable via `driveApplication.drive.academicYear`, so
  per-drive is the granularity actually queryable without adding a redundant denormalized column.
- **`ADMISSIONS_OFFICER` was added to `UserRole`** — no admissions-specific role existed anywhere
  in the docs (unlike `PLACEMENT_OFFICER`, which was already present), so one was added following
  the exact same pattern as `FEE_COLLECTION_CLERK`'s addition in `module/finance`.
- **Company CRUD lives inside `PlacementDriveService`, not a separate `CompanyService`** —
  08-system-architecture.md's file tree names only `PlacementDriveService`/
  `PlacementAnalyticsService` for this module, so company management was folded into the former
  rather than adding an unlisted third service.
- **No dedicated request/response shapes, endpoint paths, or auth role combinations are
  documented** for either module — chosen for consistency with the rest of the API, not
  transcribed from a spec, same situation as `module/hostel`/`module/transport`.

**Before writing any code, read the relevant planning document(s) below.** They are the actual
specification for this system — module boundaries, entity fields, API contracts, permission rules,
and folder layout are all defined there in detail, and code must conform to them rather than to
ad-hoc decisions made mid-task.

| Doc | Contents |
|---|---|
| `01-project-vision.md` | Problem statement, goals, guiding principles (DRY, modularity, audit-everything, air-gap-first, append-only, security-by-default), Phase 1/2 module scope |
| `02-prd.md` | User personas, user stories per module, functional/non-functional requirements, permission matrix summary, acceptance criteria |
| `03-srs.md` | Formal functional requirements per module, external interfaces, data retention/integrity/privacy rules, system constraints |
| `04-database-schema.md` | Full table-by-table schema for every module, naming conventions, append-only trigger design, index summary, seed data |
| `05-api-documentation.md` | REST API conventions and the full endpoint list per module (request/response shapes, status codes) |
| `06-design-document.md` | Backend package layout, module internal structure, cross-module communication patterns, frontend structure, security/data-flow/job/notification/storage design |
| `07-coding-standards.md` | Enforced Java/SQL/JavaScript style rules, naming conventions, error handling, logging, testing, git workflow — **violations block PR merge** |
| `08-system-architecture.md` | Architecture overview, tech stack, deployment modes, **complete backend folder tree, complete frontend folder tree, complete migration list** (§10–12), ADRs |

For "what does the exact file tree look like" or "what fields does table X have", go to
`08-system-architecture.md` §10–12 or `04-database-schema.md` directly rather than guessing —
they are exhaustive.

### Remaining scaffold vs. spec gaps

`pom.xml` now matches the docs on package (`com.college.erp`), driver (PostgreSQL, not MySQL), and
Java version (21). `application.yaml` now configures `spring.datasource.url/username/password`
(env-overridable via `DB_URL`/`DB_USERNAME`/`DB_PASSWORD`, defaulting to
`jdbc:postgresql://localhost:5432/college_erp` / `erp_user` / `erp_pass` per
`08-system-architecture.md` §13.2) and sets `spring.jpa.hibernate.ddl-auto: none` +
`open-in-view: false` — schema is owned by Flyway migrations, never by Hibernate, and lazy loading
must not silently work outside a transaction. **A running local PostgreSQL instance matching those
defaults (or matching env vars) is now required for `./mvnw test` / `spring-boot:run` to start.**

Flyway (`flyway-core` + `flyway-database-postgresql`) is now on the classpath and
`V001__create_user_tables.sql` exists (`users`, `role_assignments`, `permission_grants`,
`refresh_tokens`, verbatim from `04-database-schema.md` §5). It runs automatically on startup —
Spring Boot's Flyway autoconfiguration needs no extra properties beyond the datasource already in
`application.yaml`. `role_assignments.department_id` and `permission_grants.scope_department_id`/
`scope_batch_id` are intentionally left without an FK constraint, since `departments`/`batches`
don't exist until V003 — add those FKs in a later migration once those tables land, not by editing
V001 (migrations are forward-only, never edit a released one). `V002__create_audit_log.sql` now
exists too (audit_log + the shared `fn_reject_modification()` trigger function, reused by every
later append-only table). V003–V014 (academic, student, attendance, examination, finance, library,
asset, notification/scheduler, hr, hostel/transport, communication, seed data — full list in
`08-system-architecture.md` §12) do not exist yet; add each alongside the module code that needs it.

`pom.xml` parent is now pinned to `spring-boot-starter-parent:3.3.5`, resolving the last open gap —
it was `4.1.0` (Jackson 3 under the renamed `tools.jackson.*` package, Boot-4-only starter artifact
names like `spring-boot-starter-webmvc`) and broke as soon as real Jackson/Security config code was
written against the docs' Boot-3.3.x-era APIs. Swapped `spring-boot-starter-webmvc` →
`spring-boot-starter-web`, `spring-boot-starter-data-jpa-test` + `spring-boot-starter-webmvc-test` →
`spring-boot-starter-test` (Boot 3.x doesn't have per-starter test artifacts). If a future `pom.xml`
change reintroduces Boot 4.x, expect this to resurface.

### This machine's JDK is unusually new (26) — two build workarounds in `pom.xml`

Neither is Boot-version-specific; both are pinned to work around this specific machine's JDK, not
a project requirement:
- `<lombok.version>1.18.46</lombok.version>` override — Boot 3.3.5's managed Lombok (1.18.32)
  can't patch JDK 26's javac internals (`ExceptionInInitializerError` on `TypeTag`).
- `maven-surefire-plugin` sets `-Dnet.bytebuddy.experimental=true` — Mockito's bundled Byte Buddy
  doesn't officially support JDK 26 yet and refuses to mock any class without this flag.
- `archunit-junit5` is pinned to `1.4.1` (not the `1.3.0` first tried) and
  `src/test/resources/archunit.properties` sets
  `archunit.resolveMissingDependenciesFromClassPath=false` — even 1.4.1's bundled ASM can't parse
  JDK 26's own runtime class files (`Unsupported class file major version 70` reading
  `jrt:/java.base/java/lang/Integer.class`) and logs a WARN every run. **This warning is harmless
  noise, not a sign the rules aren't working** — verified by temporarily emptying
  `ModuleBoundaryTest`'s `ALLOWED_EXCEPTIONS` and confirming it correctly fails with the exact
  admin→auth violation (146 dependencies resolved, `UserRepository` found) before restoring it.
  Don't "fix" the WARN by suppressing logs; if `no_cross_module_repository_access` is ever
  reporting suspiciously zero violations, re-run that same empty-exceptions sanity check before
  trusting it.

All three are safe to drop once the toolchain (or this repo's target JDK) catches up.

Not verified in this environment: no local PostgreSQL instance was reachable here, so Flyway
actually applying V001/V002 against a live database, and `spring-boot:run` starting end-to-end,
are unconfirmed — only that the project compiles, all unit tests pass, and the SQL matches the
schema doc exactly. Run `./mvnw spring-boot:run` yourself against real PostgreSQL before trusting
the auth endpoints fully.

## Commands

The frontend doesn't exist yet, so only backend commands currently apply. Run from `backend/college/`.

```bash
./mvnw compile                 # compile
./mvnw test                    # unit tests (JUnit 5 + Mockito)
./mvnw test -Dtest=ClassName            # single test class
./mvnw test -Dtest=ClassName#methodName # single test method
./mvnw verify                  # unit + integration tests (Testcontainers, once added)
./mvnw spring-boot:run         # run the app locally
./mvnw checkstyle:check        # style enforcement (per 07-coding-standards.md, once configured)
./mvnw spotbugs:check          # static bug detection (once configured)
```

Once the frontend is scaffolded per `08-system-architecture.md` §11, its commands (per
`07-coding-standards.md` §16.2) will be pnpm-based: `pnpm dev`, `pnpm build`, `pnpm lint`,
`pnpm test` (Vitest + React Testing Library), run from the frontend root.

## Architecture (target — see docs for full detail)

### Style: modular monolith, not microservices

One deployable Spring Boot JAR, internally partitioned into strict feature modules, backed by one
PostgreSQL database. This is deliberate (ADR-001): the system must be deployable on an air-gapped
campus network (single JAR + single DB), and cross-module operations like fee collection need real
ACID transactions rather than sagas. Three-tier: React SPA (static, served by Nginx) → Spring Boot →
PostgreSQL.

### Module boundary rules (enforced by ArchUnit)

1. A module's JPA repositories are package-private — no other module imports them.
2. Cross-module data access goes through the other module's **service interface**, never its repository.
3. Module dependencies form a DAG (no cycles) — see the dependency graph in `08-system-architecture.md` §3.2.
4. Controllers never touch repositories; all business logic lives in services.
5. For "trigger something but don't need the result" cross-module cases, publish a Spring
   `ApplicationEvent` instead of a direct call (e.g. library issues a book → publishes
   `BookIssuedEvent` → notification module's `@EventListener` reacts). See `06-design-document.md` §2.4.

Every module (`module/{name}/`) has the same internal shape: `controller/`, `service/`, `dto/`,
`domain/` (entities + their repositories co-located). The platform kernel (`auth`, `audit`,
`notification`, `scheduler`, `storage`, `common`, `security`) sits below all feature modules and
provides shared infrastructure — never re-implement something the kernel already provides
(DateUtil, ApiResponse, PageResponse, AuditLogWriter, NotificationService, FileStorageService,
BarcodeService, etc. — see the DRY table in `06-design-document.md` §2.5).

Modules (Phase 1 / MVP): `auth`, `academic`, `student`, `attendance`, `examination`, `finance`,
`library`, `asset`, `hr`, `communication`, `admin`. Phase 2 adds `hostel`, `transport`,
`admissions`, `placement`.

### Data model conventions (see `04-database-schema.md` §1, §17)

- All PKs are `UUID`, generated app-side or via `gen_random_uuid()`.
- All business-data tables use soft delete (`is_deleted BOOLEAN`) — never physical `DELETE`.
- Four tables are **append-only, enforced by PostgreSQL triggers** that reject `UPDATE`/`DELETE`:
  `audit_log`, `ledger_entries` (fully immutable), `asset_custody_log` (fully immutable),
  `library_custody_log` (core issue fields locked, return fields settable once). Corrections are
  reversing entries, never edits (ADR-005) — this is deliberately enforced at the DB level because
  application-level enforcement is bypassable.
- `enrollments` is versioned/append-only, not overwritten — every section transfer or promotion adds a row.
- All money is `NUMERIC(14,2)`; all timestamps are `TIMESTAMPTZ`. Never `FLOAT`/`DOUBLE` for money, never `TIMESTAMP` without timezone.
- Flyway migrations: `V{n}__{description}.sql`, sequential, one concern per file, never edit a released migration.

### Security architecture (see `08-system-architecture.md` §4, `06-design-document.md` §4)

- JWT (HS256, 15 min access token, 7-day refresh token stored as SHA-256 hash) contains only user
  id/username/email — **no roles or permissions embedded** (ADR-004), so a role change or grant
  revocation takes effect on the next request without needing token invalidation.
- Permission evaluation is three-layer: (1) does the user's role template unconditionally grant this
  permission → (2) if not, does an active, non-expired scoped `permission_grant` cover it (e.g. an
  Accountant granted access to one department + one fee head) → (3) otherwise deny (403). Grants can
  only be issued by specific superior roles (Head Accountant → Accountant, Chief Librarian → Library
  Assistant, etc.) and a user can never grant themselves a permission.
- Every controller endpoint requires an explicit `@PreAuthorize` — there is no "authenticated is
  enough" default.

### API conventions (see `05-api-documentation.md` §1)

- `/api/v1/{module}/{resource}[/{id}[/{sub-resource}]]`
- Success responses: `{ success, message, data, timestamp }`. Paginated: `data` is
  `{ content, page, size, total_elements, total_pages, first, last }` (`?page=&size=`).
- Errors follow RFC 7807 (`type, title, status, detail, instance, request_id, timestamp, field_errors`).
- Financial write endpoints accept an `Idempotency-Key` header; replays return the original response.

### Deployment modes

The same codebase runs in three modes selected by config/profile, not by code branching: `cloud`
(multi-instance, S3, RLS multi-tenancy), `onprem` (single instance, MinIO or local storage),
`airgap` (single instance, local filesystem only, no outbound HTTP, email/SMS disabled by default).
Keep this in mind when adding any feature with an external dependency — it must degrade gracefully
with a feature flag, never hard-fail, when offline (P4 in `01-project-vision.md`).

## Non-negotiable coding rules

Full detail and code examples are in `07-coding-standards.md`; the highlights that most affect
where code goes:

- Controllers are thin (translate HTTP ↔ service call, `@Valid` + `@PreAuthorize` on everything, no
  business logic, no repository access, never return entities directly — use DTOs).
- Services: `@RequiredArgsConstructor` constructor injection only (no field `@Autowired`),
  `@Transactional` on writes / `@Transactional(readOnly = true)` on reads, one service method = one
  transaction boundary, throw typed exceptions (never raw `RuntimeException`), write to
  `AuditLogWriter` for every create/update/delete.
- Entities: all associations `FetchType.LAZY` (never `EAGER`), every `@Column` specifies
  `name`/`nullable`/`length`.
- No cross-module repository access — go through the other module's service (§4.4 in the standards doc).
- Never return unbounded `List` from a repository/list endpoint — paginate.
- JS/React: ES modules only, `@/` path alias, TanStack Query for server state, Zustand for UI state
  only (never server data in Zustand), named exports for shared components, `use{Name}` hook naming.
