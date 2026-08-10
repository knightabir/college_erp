# College ERP System — System Architecture Document

**Version:** 1.0  
**Date:** June 2026  
**Status:** Approved  
**Audience:** Engineering Team, DevOps, Technical Leads

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Technology Stack](#2-technology-stack)
3. [Component Architecture](#3-component-architecture)
4. [Security Architecture](#4-security-architecture)
5. [Data Architecture](#5-data-architecture)
6. [Background Processing Architecture](#6-background-processing-architecture)
7. [Notification Architecture](#7-notification-architecture)
8. [File Storage Architecture](#8-file-storage-architecture)
9. [Deployment Architecture](#9-deployment-architecture)
10. [Complete Backend Folder Structure](#10-complete-backend-folder-structure)
11. [Complete Frontend Folder Structure](#11-complete-frontend-folder-structure)
12. [Complete Database Migration Structure](#12-complete-database-migration-structure)
13. [Infrastructure and DevOps](#13-infrastructure-and-devops)
14. [Cross-Cutting Concerns](#14-cross-cutting-concerns)
15. [Architecture Decision Records](#15-architecture-decision-records)

---

## 1. Architecture Overview

### 1.1 Style: Modular Monolith

The system is a **Modular Monolith** — one deployable unit internally partitioned into strict, independently-maintainable feature modules. This is a deliberate choice over microservices, justified by:

- **Air-gap requirement**: A single JAR + single database is trivially deployable on an air-gapped network. A microservices mesh with a service registry, mTLS, distributed tracing, and multiple runtime containers would be operationally impossible for a college's IT team.
- **ACID transactions**: Fee collection must atomically update the invoice, create the payment record, and post two ledger entries. In a monolith this is one `@Transactional` method. In microservices it requires Sagas — far more complex.
- **Team size**: A modular monolith fits teams of 4–15 engineers. Clear module boundaries provide team autonomy without the operational overhead of distributed systems.
- **Future optionality**: Module boundaries are drawn so that any module can be extracted into a standalone service later — without a rewrite.

### 1.2 Three-Tier Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│  TIER 1 — PRESENTATION                                               │
│                                                                      │
│  React 18 SPA + Vite                                                 │
│  Served as static files by Nginx                                     │
│  Zero runtime Node.js in production                                  │
│  All assets (fonts, icons, JS chunks) bundled at build time          │
│  Environment config injected at container start via env-config.js    │
└──────────────────────────────┬──────────────────────────────────────┘
                               │ HTTPS  /api/v1/*
                               │ Authorization: Bearer <JWT>
                               │ Content-Type: application/json
┌──────────────────────────────▼──────────────────────────────────────┐
│  TIER 2 — APPLICATION                                                │
│                                                                      │
│  Spring Boot 3.x — Single executable JAR (Java 21)                  │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  Platform Kernel (shared infrastructure)                     │    │
│  │  ┌──────────┬───────────┬──────────┬──────────┬──────────┐  │    │
│  │  │   Auth   │   RBAC   │  Audit   │  Notif.  │ Storage  │  │    │
│  │  │  + JWT   │ + Grants  │ (async)  │  (SSE+   │ (local/  │  │    │
│  │  │  + TOTP  │ + Scope   │ append   │  email+  │ minio/   │  │    │
│  │  │          │           │  only)   │  sms)    │  s3)     │  │    │
│  │  └──────────┴───────────┴──────────┴──────────┴──────────┘  │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  ┌──────────┬──────────┬──────────┬──────────┬──────────────────┐  │
│  │ academic │ student  │attendance│   exam   │     finance      │  │
│  ├──────────┼──────────┼──────────┼──────────┼──────────────────┤  │
│  │ library  │  asset   │    hr    │  hostel  │    transport     │  │
│  ├──────────┼──────────┼──────────┼──────────┼──────────────────┤  │
│  │admissions│ placement│  comms   │  admin   │    scheduler     │  │
│  └──────────┴──────────┴──────────┴──────────┴──────────────────┘  │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  Web Layer                                                   │    │
│  │  GlobalExceptionHandler · RequestIdFilter · TenantFilter     │    │
│  └─────────────────────────────────────────────────────────────┘    │
└──────────────────────────────┬──────────────────────────────────────┘
                               │ JDBC (HikariCP connection pool)
┌──────────────────────────────▼──────────────────────────────────────┐
│  TIER 3 — DATA                                                       │
│                                                                      │
│  PostgreSQL 15+  (primary OLTP store)                                │
│  ├── Append-only tables with trigger enforcement                     │
│  ├── Row-Level Security for multi-tenant isolation (cloud mode)      │
│  └── Flyway migrations (V001–V014+)                                  │
│                                                                      │
│  File Storage (abstracted)                                           │
│  ├── Local filesystem  (air-gap default)                             │
│  ├── MinIO             (on-prem option)                              │
│  └── AWS S3            (cloud option)                                │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 2. Technology Stack

### 2.1 Backend

| Component | Technology | Version | Rationale |
|---|---|---|---|
| Language | Java | 21 LTS | LTS release with modern features (records, pattern matching, text blocks); long support window for on-prem customers |
| Framework | Spring Boot | 3.3.x | Industry-standard, excellent ecosystem, integrates all needed components |
| Build Tool | Maven | 3.9+ | Dependency management, multi-module if needed, `mvnw` wrapper eliminates global Maven requirement |
| ORM | Spring Data JPA + Hibernate | 6.x (via Boot) | Type-safe, no raw SQL in services |
| Security | Spring Security | 6.x (via Boot) | Industry-standard; custom RBAC via method security |
| Auth | JJWT | 0.12.x | JWT creation and validation; pure Java, no network dependency |
| DB Migration | Flyway | 10.x (via Boot) | Forward-only, numbered migrations; runs on startup |
| Connection Pool | HikariCP | 5.x (via Boot) | Fastest JVM connection pool; built into Spring Boot |
| Code Reduction | Lombok | 1.18.x | Eliminate getter/setter/constructor boilerplate |
| DTO Mapping | MapStruct | 1.5.x | Compile-time DTO↔Entity mapping; no reflection at runtime |
| Barcode Gen | ZXing | 3.5.x | Pure Java barcode/QR generation; zero network, zero OS dependency |
| API Docs | springdoc-openapi | 2.5.x | Auto-generates OpenAPI 3 spec from annotations |
| Cache | Caffeine | 3.x (via Boot) | In-process; zero extra infrastructure |
| Validation | Jakarta Validation | 3.x (via Boot) | Declarative constraint annotations on DTOs |
| Testing | JUnit 5 + Mockito | 5.x / 5.x | Standard Java testing |
| Integration Test | Testcontainers | 1.19.x | Real PostgreSQL in Docker for integration tests |

### 2.2 Frontend

| Component | Technology | Version | Rationale |
|---|---|---|---|
| Language | JavaScript (ES2022+) | — | Standard web language; JSDoc for type hints |
| Framework | React | 18.x | Component model, hooks, concurrent features |
| Build Tool | Vite | 5.x | Fast HMR in dev; optimized production bundles |
| Server State | TanStack Query | 5.x | Caching, background refresh, loading/error states for API data |
| UI State | Zustand | 4.x | Minimal, unopinionated; no boilerplate |
| UI Library | MUI (Material UI) | 5.x | Complete component library; vendored fonts |
| Router | React Router | 6.x | Nested routes, lazy loading, code splitting |
| HTTP Client | Axios | 1.x | Interceptor support for JWT injection and refresh |
| Styling | MUI sx + CSS Modules | — | Component-level styles; no global CSS conflicts |

### 2.3 Database and Infrastructure

| Component | Technology | Notes |
|---|---|---|
| Database | PostgreSQL | 15+; only supported engine |
| Reverse Proxy | Nginx | Serves frontend, proxies /api to backend |
| Containerization | Docker + Docker Compose | v2 plugin syntax; primary deployment mechanism |
| File Storage (default) | Local filesystem | Zero extra infrastructure; air-gap safe |
| File Storage (on-prem) | MinIO | Self-hosted S3-compatible |
| File Storage (cloud) | AWS S3 | Managed object storage |

---

## 3. Component Architecture

### 3.1 Module Boundary Rules

These rules are enforced by ArchUnit tests that run on every build:

1. A module's JPA repositories are package-private — no other module may import them
2. Cross-module calls use published service interfaces only
3. Module dependencies form a directed acyclic graph (no cycles)
4. Controllers never access repositories directly
5. Business logic lives exclusively in services

### 3.2 Module Dependency Graph

```
                    ┌────────────────────────────────────┐
                    │         Platform Kernel             │
                    │  auth · audit · notification        │
                    │  storage · common · scheduler       │
                    └─────────────────┬───────────────────┘
                                      │ all modules depend on platform kernel
          ┌───────────────────────────┼─────────────────────────────────────┐
          │                           │                                      │
     ┌────▼────┐              ┌───────▼──────┐                     ┌────────▼────┐
     │academic │◄─────────────│   student    │◄────────────────────│ admissions  │
     └────┬────┘              └───────┬──────┘                     └─────────────┘
          │                           │
     ┌────▼─────────────────────┐    │
     │        attendance        │◄───┘
     └────┬─────────────────────┘
          │
     ┌────▼─────────────────────┐
     │       examination        │
     └──────────────────────────┘

     ┌────────────────────────────────────────────────────┐
     │                   finance                          │
     │  depends on: student (for invoice generation)      │
     │  depends on: academic (for batch/term context)     │
     └────────────────────────────────────────────────────┘

     ┌───────────┐  ┌───────────┐  ┌────────────┐  ┌──────────┐
     │  library  │  │   asset   │  │     hr     │  │  hostel  │
     │(self-cont)│  │(self-cont)│  │(self-cont) │  │ →student │
     └───────────┘  └───────────┘  └────────────┘  └──────────┘

     ┌───────────────┐  ┌──────────────────┐  ┌────────────────┐
     │  communication│  │    placement     │  │   transport    │
     │  (self-cont)  │  │  → student,acad  │  │  → student     │
     └───────────────┘  └──────────────────┘  └────────────────┘
```

---

## 4. Security Architecture

### 4.1 Authentication Flow

```
[Client] ──POST /auth/login──► [JwtAuthenticationFilter bypasses]
                                ► [AuthController]
                                ► [AuthService.login()]
                                    1. Load User by username/email
                                    2. BCrypt verify (cost=12)
                                    3. Check enabled/not locked
                                    4. Load active RoleAssignments
                                    5. Generate JWT (HS256, 15 min)
                                    6. Generate random refresh token (64 bytes)
                                    7. Store SHA-256(refreshToken) in DB
                                    8. Log LOGIN to audit log (async)
                               ◄ {access_token, refresh_token}

[Client] ──GET /api/v1/*──────► [JwtAuthenticationFilter]
         Bearer <JWT>               1. Extract token from Authorization header
                                    2. Validate JWT signature + expiry
                                    3. Extract user_id from claims
                                    4. Load UserPrincipal from DB
                                    5. Set SecurityContext
                                ► [Controller @PreAuthorize]
                                    6. Evaluate role + scope permissions
                                    7. Execute if authorized, else 403
```

### 4.2 Permission Evaluation

Three-layer resolution:

```
Layer 1: ROLE TEMPLATE CHECK
  Does the user's role unconditionally grant this permission?
  (e.g., HEAD_ACCOUNTANT → FINANCE.FEE_COLLECTION = FULL)
  → If YES: grant access

Layer 2: CONDITIONAL GRANT CHECK (for scoped roles)
  Does the user have an active, non-expired permission_grant record
  where permission_key matches AND scope dimensions match?
  (e.g., ACCOUNTANT + grant for CSE dept + TUITION fee_head)
  → If YES: grant access

Layer 3: DENY
  → Return 403 Forbidden
```

### 4.3 Security Headers

Nginx adds these headers to every response:

```nginx
add_header X-Content-Type-Options "nosniff" always;
add_header X-Frame-Options "DENY" always;
add_header X-XSS-Protection "1; mode=block" always;
add_header Referrer-Policy "strict-origin-when-cross-origin" always;
add_header Content-Security-Policy
  "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline';" always;
add_header Strict-Transport-Security
  "max-age=31536000; includeSubDomains" always;
```

---

## 5. Data Architecture

### 5.1 Key Design Decisions

**UUID Primary Keys:** All tables use `UUID` PKs generated by `gen_random_uuid()`. Safe to generate application-side. No sequential information leakage.

**Append-Only Tables:** Four tables are physically immutable via PostgreSQL triggers:
- `audit_log` — fully immutable
- `ledger_entries` — fully immutable (corrections = reversing entries)
- `asset_custody_log` — fully immutable
- `library_custody_log` — partially immutable (core issue fields locked; return fields settable once)

**Soft Delete:** All master-data tables use `is_deleted BOOLEAN NOT NULL DEFAULT FALSE`. Physical DELETEs are never executed on business data.

**Enrollment as Append-Only:** The `enrollments` table is versioned, not overwritten. Every section transfer, batch promotion, or term change creates a new row. History is permanent.

### 5.2 Multi-Tenancy (Cloud Mode)

Cloud deployments use PostgreSQL Row-Level Security:

```sql
-- Session variable set by TenantFilter on every request
SET app.current_tenant = '<institution_uuid>';

-- Policy on every tenant-scoped table
CREATE POLICY tenant_isolation ON students
    USING (tenant_id = current_setting('app.current_tenant')::UUID);

ALTER TABLE students ENABLE ROW LEVEL SECURITY;
```

Single-tenant (on-prem/air-gap) deployments use the same schema with a single fixed tenant row — no code changes between modes.

---

## 6. Background Processing Architecture

### 6.1 Design: Database-Backed Queue

```
┌─────────────────────────────────────────────────┐
│  background_jobs table (PostgreSQL)              │
│  status: PENDING → RUNNING → DONE / FAILED       │
└───────────────────────┬─────────────────────────┘
                        │ SELECT ... FOR UPDATE SKIP LOCKED
                        │ (safe for multiple app instances)
               ┌────────▼────────┐
               │  JobDispatcher  │  @Scheduled every 30 seconds
               │  (Spring bean)  │
               └────────┬────────┘
                        │ dispatch by job_type
        ┌───────────────┼───────────────────────┐
        │               │                       │
┌───────▼──────┐ ┌──────▼──────┐  ┌────────────▼──────────┐
│AttendanceScan│ │FeeReminder  │  │LibraryOverdueScan      │
│Job           │ │Job          │  │Job                     │
└──────────────┘ └─────────────┘  └────────────────────────┘
```

### 6.2 Scheduled Cron Triggers

```
01:00 daily  → AttendanceShortfallScanJob
02:00 daily  → LibraryOverdueScanJob
03:00 daily  → PermissionGrantExpiryJob
08:00 daily  → FeeDueDateReminderJob
09:00 Monday → MaintenanceRenewalAlertJob
02:00 daily  → Cleanup DONE jobs older than 7 days
```

---

## 7. Notification Architecture

```
Any Service
    │
    │ notificationService.send(request)  [@Async — non-blocking]
    │
    ▼
NotificationService
    │
    ├──► 1. INSERT notification_records (in-app inbox) — always
    │
    ├──► 2. SseNotificationPusher.push() — if user has open SSE connection
    │
    ├──► 3. EmailChannel.send() — if email.enabled = true
    │          Failure here does NOT roll back steps 1–2
    │
    └──► 4. SmsChannel.send() — if sms.enabled = true
               Failure here does NOT roll back steps 1–3

SSE Connection (real-time):
    [Client] GET /api/v1/notifications/stream
    [Server] SseEmitterRegistry.create(userId) → SseEmitter
    [On notification] SseNotificationPusher.push(userId, record)
    [On disconnect] SseEmitterRegistry.remove(userId)
    [Fallback] If no SSE connection: notification is in DB inbox for next login
```

---

## 8. File Storage Architecture

```
Any Module (Library, Asset, Student, Exam...)
    │
    │ fileStorageService.store(request, scope)
    │
    ▼
FileStorageService (interface)
    │
    ├── LocalFileStorageService   ← active when erp.storage.mode=local (default/air-gap)
    │   Saves to: {base-path}/{scope}/{uuid}.{ext}
    │   Returns: fileId (UUID string)
    │
    ├── MinioFileStorageService   ← active when erp.storage.mode=minio
    │   Saves to MinIO bucket: {scope}/{uuid}.{ext}
    │
    └── S3FileStorageService      ← active when erp.storage.mode=s3
        Saves to S3 bucket: {scope}/{uuid}.{ext}

File Retrieval:
    GET /api/v1/files/{fileId}
    → FileController → fileStorageService.retrieve(fileId)
    → In air-gap: streams bytes through app
    → In cloud: redirects to presigned S3 URL
```

---

## 9. Deployment Architecture

### 9.1 Cloud Deployment

```
Internet
    │
[WAF + Load Balancer]
    │
[Nginx] ──── serves ──► React static build (CDN-cached)
    │
    └─/api/───► [Spring Boot Instance 1] ┐
                [Spring Boot Instance 2] ├── load balanced, stateless
                [Spring Boot Instance N] ┘
                        │
               [PostgreSQL Primary] ──► [Read Replica (reports)]
               [S3 Bucket]
               [Managed Redis (optional — for distributed cache)]
```

### 9.2 On-Premises Deployment

```
Campus Network (internet access present, used selectively)
    │
[Institution Firewall]
    │
[Nginx] ─── serves ──► React static build
    │
    └─/api/───► [Spring Boot — single instance or small cluster]
                        │
               [PostgreSQL — on customer hardware]
               [Local filesystem or MinIO for file storage]
               [SMTP relay if customer has mail server]
```

### 9.3 Air-Gapped Deployment

```
Isolated Campus Network (zero outbound internet)
    │
[Nginx — TLS via self-signed/internal CA cert]
    │
[Nginx] ─── serves ──► React static build (all assets bundled, no CDN)
    │
    └─/api/───► [Spring Boot — single instance]
                        │
               [PostgreSQL — on customer hardware]
               [Local filesystem — file storage]
               [Internal SMTP if available, else in-app only]
               [Local NTP or manual clock]

Update mechanism:
    Admin uploads signed offline bundle (.tar.gz)
    → Docker images loaded via docker load
    → Services restarted
    → Flyway migrations run automatically on startup
```

### 9.4 Docker Compose Structure

```yaml
# docker-compose.yml (base)
services:
  postgres:
    image: postgres:15
    volumes: [postgres_data:/var/lib/postgresql/data]
    healthcheck: [pg_isready]

  backend:
    image: college-erp-backend:${VERSION}
    depends_on: { postgres: { condition: service_healthy } }
    environment:
      SPRING_PROFILES_ACTIVE: ${DEPLOY_MODE}  # cloud | onprem | airgap
      DB_URL, DB_USERNAME, DB_PASSWORD
      JWT_SECRET, STORAGE_MODE, ...

  frontend:
    image: college-erp-frontend:${VERSION}
    depends_on: [backend]
    environment:
      API_BASE_URL, DEPLOYMENT_MODE, ...
    ports: ["80:80", "443:443"]

# Activate with: DEPLOY_MODE=airgap docker compose up -d
```

---

## 10. Complete Backend Folder Structure

```
college-erp/                                    ← Project root
│
├── pom.xml                                     ← Maven POM (Java 21, Spring Boot 3.3.x)
├── README.md
│
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/
    │   │       └── college/
    │   │           └── erp/
    │   │               │
    │   │               ├── ErpApplication.java                 ← @SpringBootApplication entry
    │   │               │
    │   │               ├── config/                             ← Spring @Configuration classes
    │   │               │   ├── SecurityConfig.java             ← Filter chain, CORS, auth provider
    │   │               │   ├── JwtProperties.java              ← @ConfigurationProperties jwt.*
    │   │               │   ├── ErpProperties.java              ← @ConfigurationProperties erp.*
    │   │               │   ├── CorsConfig.java                 ← CorsConfigurationSource bean
    │   │               │   ├── AsyncConfig.java                ← ThreadPoolTaskExecutor, @EnableAsync
    │   │               │   ├── AuditConfig.java                ← @EnableJpaAuditing, AuditorAware
    │   │               │   ├── JacksonConfig.java              ← ObjectMapper: snake_case, ISO dates
    │   │               │   └── OpenApiConfig.java              ← springdoc OpenAPI bean
    │   │               │
    │   │               ├── common/                             ← Shared code — no domain logic
    │   │               │   ├── domain/
    │   │               │   │   └── BaseEntity.java             ← @MappedSuperclass: id, timestamps
    │   │               │   ├── dto/
    │   │               │   │   ├── ApiResponse.java            ← {success, message, data, timestamp}
    │   │               │   │   ├── PageResponse.java           ← Wraps Spring Data Page<T>
    │   │               │   │   ├── CursorPageResponse.java     ← Cursor-based pagination
    │   │               │   │   └── ErrorResponse.java          ← RFC 7807 Problem Details
    │   │               │   ├── exception/
    │   │               │   │   ├── ErpException.java           ← Base: message + HttpStatus
    │   │               │   │   ├── ResourceNotFoundException.java   ← → HTTP 404
    │   │               │   │   ├── ValidationException.java         ← → HTTP 400
    │   │               │   │   ├── DuplicateResourceException.java  ← → HTTP 409
    │   │               │   │   ├── BusinessRuleViolationException.java ← → HTTP 422
    │   │               │   │   └── AccessDeniedException.java       ← → HTTP 403
    │   │               │   └── util/
    │   │               │       ├── DateUtil.java               ← Format, parse, calculate dates
    │   │               │       ├── SlugUtil.java               ← Generate URL-safe slugs
    │   │               │       └── PaginationUtil.java         ← Build Pageable from params
    │   │               │
    │   │               ├── security/                           ← Auth + RBAC infrastructure
    │   │               │   ├── jwt/
    │   │               │   │   ├── JwtTokenProvider.java       ← Generate, validate, parse JWT
    │   │               │   │   ├── JwtAuthenticationFilter.java ← OncePerRequestFilter
    │   │               │   │   └── UserPrincipal.java          ← Implements UserDetails
    │   │               │   ├── rbac/
    │   │               │   │   ├── Permission.java             ← Enum: all permission keys
    │   │               │   │   ├── PermissionEvaluator.java    ← Custom Spring evaluator
    │   │               │   │   ├── PermissionScope.java        ← {deptId, batchId, feeHead, amount}
    │   │               │   │   ├── GrantResolver.java          ← Loads active grants for a user
    │   │               │   │   └── RoleTemplate.java           ← Default permissions per role
    │   │               │   ├── totp/
    │   │               │   │   └── TotpService.java            ← RFC 6238 TOTP verify (local)
    │   │               │   └── session/
    │   │               │       ├── SessionRegistry.java        ← Track active refresh tokens
    │   │               │       └── SessionManagementService.java ← Force logout, list sessions
    │   │               │
    │   │               ├── audit/                              ← Append-only audit trail
    │   │               │   ├── AuditLog.java                   ← @Entity (append-only table)
    │   │               │   ├── AuditLogRepository.java         ← JPA repository
    │   │               │   ├── AuditLogWriter.java             ← @Async writer (used by all modules)
    │   │               │   └── AuditAction.java                ← Enum: CREATE, FEE_COLLECTED, etc.
    │   │               │
    │   │               ├── notification/                       ← Notification system
    │   │               │   ├── NotificationService.java        ← Orchestrates all channels
    │   │               │   ├── NotificationRecord.java         ← @Entity (in-app inbox)
    │   │               │   ├── NotificationRepository.java
    │   │               │   ├── NotificationRequest.java        ← Request DTO
    │   │               │   ├── channel/
    │   │               │   │   ├── NotificationChannel.java    ← Interface
    │   │               │   │   ├── InAppChannel.java           ← Always enabled
    │   │               │   │   ├── EmailChannel.java           ← @ConditionalOnProperty
    │   │               │   │   └── SmsChannel.java             ← @ConditionalOnProperty (off in airgap)
    │   │               │   ├── template/
    │   │               │   │   ├── NotificationTemplate.java   ← @Entity
    │   │               │   │   ├── NotificationTemplateRepository.java
    │   │               │   │   └── TemplateRenderer.java       ← Variable substitution
    │   │               │   └── sse/
    │   │               │       ├── SseEmitterRegistry.java     ← ConcurrentHashMap<UUID, SseEmitter>
    │   │               │       └── SseNotificationPusher.java  ← Push to open SSE streams
    │   │               │
    │   │               ├── scheduler/                          ← Background job system
    │   │               │   ├── BackgroundJob.java              ← @Entity (job queue table)
    │   │               │   ├── BackgroundJobRepository.java    ← SKIP LOCKED query
    │   │               │   ├── JobDispatcher.java              ← @Scheduled poller + router
    │   │               │   ├── ScheduledTasks.java             ← Cron triggers → enqueue jobs
    │   │               │   └── jobs/
    │   │               │       ├── AttendanceShortfallScanJob.java
    │   │               │       ├── FeeDueDateReminderJob.java
    │   │               │       ├── LibraryOverdueScanJob.java
    │   │               │       ├── PermissionGrantExpiryJob.java
    │   │               │       ├── BackupVerificationJob.java
    │   │               │       └── MaintenanceRenewalAlertJob.java
    │   │               │
    │   │               ├── storage/                            ← File storage abstraction
    │   │               │   ├── FileStorageService.java         ← Interface: store, retrieve, delete
    │   │               │   ├── StoredFile.java                 ← Response: fileId, url, size
    │   │               │   ├── FileUploadRequest.java          ← Request: fileName, contentType, data
    │   │               │   ├── StorageScope.java               ← Enum: STUDENT_DOC, LIBRARY_ASSET...
    │   │               │   └── impl/
    │   │               │       ├── LocalFileStorageService.java  ← @ConditionalOnProperty(mode=local)
    │   │               │       ├── MinioFileStorageService.java  ← @ConditionalOnProperty(mode=minio)
    │   │               │       └── S3FileStorageService.java     ← @ConditionalOnProperty(mode=s3)
    │   │               │
    │   │               ├── web/                                ← Servlet layer
    │   │               │   ├── GlobalExceptionHandler.java     ← @RestControllerAdvice
    │   │               │   └── RequestIdFilter.java            ← Injects X-Request-Id into MDC
    │   │               │
    │   │               ├── module/                             ← All feature modules
    │   │               │   │
    │   │               │   ├── auth/
    │   │               │   │   ├── controller/
    │   │               │   │   │   ├── AuthController.java     ← login, logout, refresh, change-pwd
    │   │               │   │   │   └── TotpController.java     ← enroll, verify TOTP
    │   │               │   │   ├── service/
    │   │               │   │   │   ├── AuthService.java        ← login, refresh, logout, pwd-change
    │   │               │   │   │   ├── UserDetailsServiceImpl.java ← Spring Security UserDetailsService
    │   │               │   │   │   └── RefreshTokenService.java
    │   │               │   │   ├── dto/
    │   │               │   │   │   ├── LoginRequest.java
    │   │               │   │   │   ├── LoginResponse.java
    │   │               │   │   │   ├── RefreshTokenRequest.java
    │   │               │   │   │   ├── TokenResponse.java
    │   │               │   │   │   └── ChangePasswordRequest.java
    │   │               │   │   └── domain/
    │   │               │   │       ├── User.java
    │   │               │   │       ├── UserRepository.java
    │   │               │   │       ├── UserRole.java           ← Enum: all 24 roles
    │   │               │   │       ├── RoleAssignment.java
    │   │               │   │       ├── RoleAssignmentRepository.java
    │   │               │   │       ├── PermissionGrant.java
    │   │               │   │       ├── PermissionGrantRepository.java
    │   │               │   │       ├── RefreshToken.java
    │   │               │   │       └── RefreshTokenRepository.java
    │   │               │   │
    │   │               │   ├── academic/
    │   │               │   │   ├── controller/
    │   │               │   │   │   ├── DepartmentController.java
    │   │               │   │   │   ├── CourseController.java
    │   │               │   │   │   ├── BatchController.java
    │   │               │   │   │   ├── SectionController.java
    │   │               │   │   │   ├── SubjectController.java
    │   │               │   │   │   └── TimetableController.java
    │   │               │   │   ├── service/
    │   │               │   │   │   ├── DepartmentService.java
    │   │               │   │   │   ├── CourseService.java
    │   │               │   │   │   ├── BatchService.java
    │   │               │   │   │   ├── SectionService.java
    │   │               │   │   │   ├── SubjectService.java
    │   │               │   │   │   ├── TimetableService.java
    │   │               │   │   │   └── ConflictDetectionService.java
    │   │               │   │   ├── dto/
    │   │               │   │   │   ├── DepartmentDto.java
    │   │               │   │   │   ├── CourseDto.java
    │   │               │   │   │   ├── BatchDto.java
    │   │               │   │   │   ├── SectionDto.java
    │   │               │   │   │   ├── SubjectDto.java
    │   │               │   │   │   └── TimetableDto.java
    │   │               │   │   └── domain/
    │   │               │   │       ├── Department.java
    │   │               │   │       ├── DepartmentRepository.java
    │   │               │   │       ├── Course.java
    │   │               │   │       ├── CourseRepository.java
    │   │               │   │       ├── CourseLevel.java         ← Enum: UG, PG, DIPLOMA, PHD
    │   │               │   │       ├── AcademicYear.java
    │   │               │   │       ├── AcademicYearRepository.java
    │   │               │   │       ├── Term.java
    │   │               │   │       ├── TermRepository.java
    │   │               │   │       ├── Batch.java
    │   │               │   │       ├── BatchRepository.java
    │   │               │   │       ├── Section.java
    │   │               │   │       ├── SectionRepository.java
    │   │               │   │       ├── Subject.java
    │   │               │   │       ├── SubjectRepository.java
    │   │               │   │       ├── SubjectType.java         ← Enum: THEORY, LAB, PROJECT...
    │   │               │   │       ├── SubjectOffering.java
    │   │               │   │       ├── SubjectOfferingRepository.java
    │   │               │   │       ├── FacultySubjectMapping.java
    │   │               │   │       └── FacultySubjectMappingRepository.java
    │   │               │   │
    │   │               │   ├── student/
    │   │               │   │   ├── controller/
    │   │               │   │   │   ├── StudentController.java
    │   │               │   │   │   ├── EnrollmentController.java
    │   │               │   │   │   └── StudentDocumentController.java
    │   │               │   │   ├── service/
    │   │               │   │   │   ├── StudentService.java
    │   │               │   │   │   ├── EnrollmentService.java
    │   │               │   │   │   ├── DocumentVerificationService.java
    │   │               │   │   │   ├── AlumniService.java
    │   │               │   │   │   └── StudentSecurityService.java  ← @PreAuthorize helper
    │   │               │   │   ├── dto/
    │   │               │   │   │   ├── StudentDto.java
    │   │               │   │   │   ├── StudentSummaryDto.java
    │   │               │   │   │   ├── EnrollmentDto.java
    │   │               │   │   │   ├── GuardianDto.java
    │   │               │   │   │   └── DocumentUploadDto.java
    │   │               │   │   └── domain/
    │   │               │   │       ├── Student.java
    │   │               │   │       ├── StudentRepository.java
    │   │               │   │       ├── StudentStatus.java       ← Enum: ACTIVE, GRADUATED...
    │   │               │   │       ├── Gender.java              ← Enum: MALE, FEMALE, OTHER...
    │   │               │   │       ├── Enrollment.java          ← Append-only versioned record
    │   │               │   │       ├── EnrollmentRepository.java
    │   │               │   │       ├── EnrollmentStatus.java
    │   │               │   │       ├── Guardian.java
    │   │               │   │       ├── GuardianRepository.java
    │   │               │   │       ├── StudentDocument.java
    │   │               │   │       └── StudentDocumentRepository.java
    │   │               │   │
    │   │               │   ├── attendance/
    │   │               │   │   ├── controller/
    │   │               │   │   │   └── AttendanceController.java
    │   │               │   │   ├── service/
    │   │               │   │   │   ├── AttendanceService.java
    │   │               │   │   │   ├── AttendanceCaptureService.java
    │   │               │   │   │   ├── AttendanceRulesEngine.java
    │   │               │   │   │   └── CondonationService.java
    │   │               │   │   ├── dto/
    │   │               │   │   │   ├── AttendanceMarkDto.java
    │   │               │   │   │   ├── AttendanceSummaryDto.java
    │   │               │   │   │   ├── DefaulterDto.java
    │   │               │   │   │   └── CondonationRequestDto.java
    │   │               │   │   └── domain/
    │   │               │   │       ├── AttendanceSession.java
    │   │               │   │       ├── AttendanceSessionRepository.java
    │   │               │   │       ├── SessionStatus.java       ← Enum: DRAFT, SUBMITTED, LOCKED
    │   │               │   │       ├── AttendanceRecord.java
    │   │               │   │       ├── AttendanceRecordRepository.java
    │   │               │   │       ├── AttendanceStatus.java    ← Enum: PRESENT, ABSENT, LATE...
    │   │               │   │       ├── AttendancePolicy.java
    │   │               │   │       ├── AttendancePolicyRepository.java
    │   │               │   │       ├── CondonationRequest.java
    │   │               │   │       ├── CondonationRequestRepository.java
    │   │               │   │       └── CondonationStatus.java   ← Enum: PENDING, APPROVED, REJECTED
    │   │               │   │
    │   │               │   ├── examination/
    │   │               │   │   ├── controller/
    │   │               │   │   │   ├── ExamController.java
    │   │               │   │   │   ├── MarksController.java
    │   │               │   │   │   ├── ResultController.java
    │   │               │   │   │   └── HallTicketController.java
    │   │               │   │   ├── service/
    │   │               │   │   │   ├── ExamService.java
    │   │               │   │   │   ├── MarksEntryService.java
    │   │               │   │   │   ├── ResultProcessingService.java
    │   │               │   │   │   ├── GradeCardService.java
    │   │               │   │   │   ├── HallTicketService.java
    │   │               │   │   │   └── RevaluationService.java
    │   │               │   │   ├── dto/
    │   │               │   │   │   ├── ExamDto.java
    │   │               │   │   │   ├── MarksEntryDto.java
    │   │               │   │   │   ├── ResultDto.java
    │   │               │   │   │   ├── GradeCardDto.java
    │   │               │   │   │   └── RevaluationRequestDto.java
    │   │               │   │   └── domain/
    │   │               │   │       ├── Exam.java
    │   │               │   │       ├── ExamRepository.java
    │   │               │   │       ├── ExamType.java
    │   │               │   │       ├── ExamSchedule.java
    │   │               │   │       ├── ExamScheduleRepository.java
    │   │               │   │       ├── HallTicket.java
    │   │               │   │       ├── HallTicketRepository.java
    │   │               │   │       ├── SeatingAllocation.java
    │   │               │   │       ├── SeatingAllocationRepository.java
    │   │               │   │       ├── InvigilatorAllocation.java
    │   │               │   │       ├── InvigilatorAllocationRepository.java
    │   │               │   │       ├── MarksEntry.java
    │   │               │   │       ├── MarksEntryRepository.java
    │   │               │   │       ├── MarksStatus.java         ← Enum: DRAFT, SUBMITTED, MODERATED, PUBLISHED
    │   │               │   │       ├── ResultRecord.java
    │   │               │   │       ├── ResultRecordRepository.java
    │   │               │   │       ├── RevaluationRequest.java
    │   │               │   │       ├── RevaluationRequestRepository.java
    │   │               │   │       ├── QuestionPaper.java
    │   │               │   │       └── QuestionPaperRepository.java
    │   │               │   │
    │   │               │   ├── finance/
    │   │               │   │   ├── controller/
    │   │               │   │   │   ├── FeeStructureController.java
    │   │               │   │   │   ├── FeeCollectionController.java
    │   │               │   │   │   ├── ScholarshipController.java
    │   │               │   │   │   ├── RefundController.java
    │   │               │   │   │   ├── LedgerController.java
    │   │               │   │   │   ├── BudgetController.java
    │   │               │   │   │   ├── ProcurementController.java
    │   │               │   │   │   ├── PayrollController.java
    │   │               │   │   │   ├── FinancialYearController.java
    │   │               │   │   │   └── AccountantGrantController.java
    │   │               │   │   ├── service/
    │   │               │   │   │   ├── FeeStructureService.java
    │   │               │   │   │   ├── FeeCollectionService.java
    │   │               │   │   │   ├── ScholarshipService.java
    │   │               │   │   │   ├── RefundService.java
    │   │               │   │   │   ├── LedgerService.java       ← Double-entry posting
    │   │               │   │   │   ├── BudgetService.java
    │   │               │   │   │   ├── ProcurementService.java
    │   │               │   │   │   ├── PayrollService.java
    │   │               │   │   │   ├── FinancialYearService.java
    │   │               │   │   │   ├── AccountantGrantService.java
    │   │               │   │   │   ├── BankReconciliationService.java
    │   │               │   │   │   ├── FinancialReportService.java
    │   │               │   │   │   ├── SegregationOfDutiesGuard.java ← SOD enforcement
    │   │               │   │   │   └── FinancialYearLockGuard.java   ← Dual-approval enforcement
    │   │               │   │   ├── dto/
    │   │               │   │   │   ├── FeeStructureDto.java
    │   │               │   │   │   ├── FeeHeadDto.java
    │   │               │   │   │   ├── FeeCollectionDto.java
    │   │               │   │   │   ├── ReceiptDto.java
    │   │               │   │   │   ├── ScholarshipDto.java
    │   │               │   │   │   ├── RefundRequestDto.java
    │   │               │   │   │   ├── LedgerEntryDto.java
    │   │               │   │   │   ├── BudgetDto.java
    │   │               │   │   │   ├── PurchaseOrderDto.java
    │   │               │   │   │   ├── VendorDto.java
    │   │               │   │   │   ├── PaySlipDto.java
    │   │               │   │   │   ├── FinancialStatementDto.java
    │   │               │   │   │   └── AccountantGrantDto.java
    │   │               │   │   └── domain/
    │   │               │   │       ├── FeeHead.java
    │   │               │   │       ├── FeeHeadRepository.java
    │   │               │   │       ├── FeeStructure.java
    │   │               │   │       ├── FeeStructureRepository.java
    │   │               │   │       ├── FeeInvoice.java
    │   │               │   │       ├── FeeInvoiceRepository.java
    │   │               │   │       ├── InvoiceStatus.java       ← Enum: UNPAID, PARTIAL, PAID...
    │   │               │   │       ├── FeePayment.java
    │   │               │   │       ├── FeePaymentRepository.java
    │   │               │   │       ├── PaymentMode.java         ← Enum: CASH, ONLINE_UPI, NEFT...
    │   │               │   │       ├── PaymentStatus.java
    │   │               │   │       ├── LedgerEntry.java         ← Append-only entity
    │   │               │   │       ├── LedgerEntryRepository.java
    │   │               │   │       ├── EntryType.java           ← Enum: DEBIT, CREDIT
    │   │               │   │       ├── AccountHead.java
    │   │               │   │       ├── AccountHeadRepository.java
    │   │               │   │       ├── AccountType.java         ← Enum: INCOME, EXPENSE, ASSET...
    │   │               │   │       ├── FinancialYear.java
    │   │               │   │       ├── FinancialYearRepository.java
    │   │               │   │       ├── FinancialYearStatus.java
    │   │               │   │       ├── AccountantPermissionGrant.java
    │   │               │   │       ├── AccountantPermissionGrantRepository.java
    │   │               │   │       ├── Vendor.java
    │   │               │   │       ├── VendorRepository.java
    │   │               │   │       ├── PurchaseOrder.java
    │   │               │   │       ├── PurchaseOrderRepository.java
    │   │               │   │       └── PurchaseOrderStatus.java
    │   │               │   │
    │   │               │   ├── library/
    │   │               │   │   ├── controller/
    │   │               │   │   │   ├── CatalogController.java
    │   │               │   │   │   ├── CirculationController.java
    │   │               │   │   │   ├── ReservationController.java
    │   │               │   │   │   ├── FineController.java
    │   │               │   │   │   └── LibraryMemberController.java
    │   │               │   │   ├── service/
    │   │               │   │   │   ├── CatalogService.java      ← Title + copy management
    │   │               │   │   │   ├── BarcodeService.java      ← ZXing Code128 + QR (no network)
    │   │               │   │   │   ├── CirculationService.java  ← Issue, return, renew
    │   │               │   │   │   ├── ReservationService.java
    │   │               │   │   │   ├── FineService.java         ← Accrual, collection, waiver
    │   │               │   │   │   ├── StockVerificationService.java
    │   │               │   │   │   ├── LibraryMemberService.java
    │   │               │   │   │   └── LibraryReportService.java
    │   │               │   │   ├── dto/
    │   │               │   │   │   ├── BookTitleDto.java
    │   │               │   │   │   ├── BookCopyDto.java
    │   │               │   │   │   ├── CustodyLogDto.java
    │   │               │   │   │   ├── IssueBookDto.java
    │   │               │   │   │   ├── ReturnBookDto.java
    │   │               │   │   │   ├── ReservationDto.java
    │   │               │   │   │   ├── FineDto.java
    │   │               │   │   │   └── BarcodeSheetDto.java
    │   │               │   │   └── domain/
    │   │               │   │       ├── BookTitle.java
    │   │               │   │       ├── BookTitleRepository.java
    │   │               │   │       ├── BookCopy.java
    │   │               │   │       ├── BookCopyRepository.java
    │   │               │   │       ├── CopyCondition.java       ← Enum: EXCELLENT, GOOD, POOR, LOST
    │   │               │   │       ├── CopyStatus.java          ← Enum: AVAILABLE, ISSUED, LOST
    │   │               │   │       ├── LibraryCustodyLog.java   ← Append-only, partial immutable
    │   │               │   │       ├── LibraryCustodyLogRepository.java
    │   │               │   │       ├── Reservation.java
    │   │               │   │       ├── ReservationRepository.java
    │   │               │   │       ├── ReservationStatus.java
    │   │               │   │       ├── LibraryFine.java
    │   │               │   │       ├── LibraryFineRepository.java
    │   │               │   │       ├── FineStatus.java
    │   │               │   │       ├── LoanPolicy.java
    │   │               │   │       ├── LoanPolicyRepository.java
    │   │               │   │       ├── LibraryMember.java
    │   │               │   │       ├── LibraryMemberRepository.java
    │   │               │   │       ├── EResource.java
    │   │               │   │       ├── EResourceRepository.java
    │   │               │   │       ├── LibraryAcquisition.java
    │   │               │   │       └── LibraryAcquisitionRepository.java
    │   │               │   │
    │   │               │   ├── asset/
    │   │               │   │   ├── controller/
    │   │               │   │   │   ├── AssetController.java
    │   │               │   │   │   ├── AssetTransferController.java
    │   │               │   │   │   ├── MaintenanceController.java
    │   │               │   │   │   └── RoomBookingController.java
    │   │               │   │   ├── service/
    │   │               │   │   │   ├── AssetService.java
    │   │               │   │   │   ├── AssetRegistryService.java
    │   │               │   │   │   ├── AssetTransferService.java
    │   │               │   │   │   ├── MaintenanceService.java
    │   │               │   │   │   ├── AssetAuditService.java
    │   │               │   │   │   ├── DepreciationService.java
    │   │               │   │   │   └── RoomBookingService.java
    │   │               │   │   ├── dto/
    │   │               │   │   │   ├── AssetDto.java
    │   │               │   │   │   ├── AssetTransferDto.java
    │   │               │   │   │   ├── MaintenanceTicketDto.java
    │   │               │   │   │   ├── AssetDisposalDto.java
    │   │               │   │   │   └── RoomBookingDto.java
    │   │               │   │   └── domain/
    │   │               │   │       ├── Asset.java
    │   │               │   │       ├── AssetRepository.java
    │   │               │   │       ├── AssetStatus.java         ← Enum: ACTIVE, DISPOSED, LOST
    │   │               │   │       ├── AssetCategory.java
    │   │               │   │       ├── AssetCategoryRepository.java
    │   │               │   │       ├── AssetCustodyLog.java     ← Append-only, fully immutable
    │   │               │   │       ├── AssetCustodyLogRepository.java
    │   │               │   │       ├── AssetTag.java
    │   │               │   │       ├── AssetTagRepository.java
    │   │               │   │       ├── MaintenanceSchedule.java
    │   │               │   │       ├── MaintenanceScheduleRepository.java
    │   │               │   │       ├── MaintenanceTicket.java
    │   │               │   │       ├── MaintenanceTicketRepository.java
    │   │               │   │       ├── TicketPriority.java
    │   │               │   │       ├── TicketStatus.java
    │   │               │   │       ├── AnnualMaintenanceContract.java
    │   │               │   │       ├── AnnualMaintenanceContractRepository.java
    │   │               │   │       ├── AssetDisposal.java
    │   │               │   │       ├── AssetDisposalRepository.java
    │   │               │   │       ├── DepreciationRecord.java
    │   │               │   │       ├── DepreciationRecordRepository.java
    │   │               │   │       ├── ConsumableItem.java
    │   │               │   │       ├── ConsumableItemRepository.java
    │   │               │   │       ├── RoomBooking.java
    │   │               │   │       └── RoomBookingRepository.java
    │   │               │   │
    │   │               │   ├── hr/
    │   │               │   │   ├── controller/
    │   │               │   │   │   ├── StaffController.java
    │   │               │   │   │   ├── LeaveController.java
    │   │               │   │   │   └── AppraisalController.java
    │   │               │   │   ├── service/
    │   │               │   │   │   ├── StaffOnboardingService.java
    │   │               │   │   │   ├── LeaveManagementService.java
    │   │               │   │   │   ├── AppraisalService.java
    │   │               │   │   │   └── WorkloadTrackingService.java
    │   │               │   │   ├── dto/
    │   │               │   │   │   ├── StaffProfileDto.java
    │   │               │   │   │   ├── LeaveRequestDto.java
    │   │               │   │   │   ├── LeaveBalanceDto.java
    │   │               │   │   │   └── AppraisalDto.java
    │   │               │   │   └── domain/
    │   │               │   │       ├── StaffProfile.java
    │   │               │   │       ├── StaffProfileRepository.java
    │   │               │   │       ├── Designation.java
    │   │               │   │       ├── DesignationRepository.java
    │   │               │   │       ├── SalaryStructure.java
    │   │               │   │       ├── SalaryStructureRepository.java
    │   │               │   │       ├── LeaveType.java
    │   │               │   │       ├── LeaveTypeRepository.java
    │   │               │   │       ├── LeaveBalance.java
    │   │               │   │       ├── LeaveBalanceRepository.java
    │   │               │   │       ├── LeaveRequest.java
    │   │               │   │       ├── LeaveRequestRepository.java
    │   │               │   │       ├── AppraisalCycle.java
    │   │               │   │       ├── AppraisalCycleRepository.java
    │   │               │   │       ├── AppraisalForm.java
    │   │               │   │       ├── AppraisalFormRepository.java
    │   │               │   │       ├── RecruitmentPost.java
    │   │               │   │       └── RecruitmentPostRepository.java
    │   │               │   │
    │   │               │   ├── hostel/
    │   │               │   │   ├── controller/
    │   │               │   │   │   ├── RoomAllocationController.java
    │   │               │   │   │   ├── MessController.java
    │   │               │   │   │   └── OutpassController.java
    │   │               │   │   ├── service/
    │   │               │   │   │   ├── RoomAllocationService.java
    │   │               │   │   │   ├── MessService.java
    │   │               │   │   │   └── OutpassService.java
    │   │               │   │   ├── dto/
    │   │               │   │   │   ├── RoomAllocationDto.java
    │   │               │   │   │   ├── MessMenuDto.java
    │   │               │   │   │   ├── OutpassRequestDto.java
    │   │               │   │   │   └── VisitorLogDto.java
    │   │               │   │   └── domain/
    │   │               │   │       ├── HostelBlock.java
    │   │               │   │       ├── HostelBlockRepository.java
    │   │               │   │       ├── Room.java
    │   │               │   │       ├── RoomRepository.java
    │   │               │   │       ├── RoomAllocation.java
    │   │               │   │       ├── RoomAllocationRepository.java
    │   │               │   │       ├── MessMenu.java
    │   │               │   │       ├── MessMenuRepository.java
    │   │               │   │       ├── MealAttendance.java
    │   │               │   │       ├── MealAttendanceRepository.java
    │   │               │   │       ├── VisitorLog.java
    │   │               │   │       ├── VisitorLogRepository.java
    │   │               │   │       ├── OutpassRequest.java
    │   │               │   │       └── OutpassRequestRepository.java
    │   │               │   │
    │   │               │   ├── transport/
    │   │               │   │   ├── controller/
    │   │               │   │   │   └── TransportController.java
    │   │               │   │   ├── service/
    │   │               │   │   │   ├── RouteManagementService.java
    │   │               │   │   │   └── GpsTrackingService.java
    │   │               │   │   ├── dto/
    │   │               │   │   │   ├── RouteDto.java
    │   │               │   │   │   ├── VehicleDto.java
    │   │               │   │   │   └── StudentRouteMappingDto.java
    │   │               │   │   └── domain/
    │   │               │   │       ├── Route.java
    │   │               │   │       ├── RouteRepository.java
    │   │               │   │       ├── RouteStop.java
    │   │               │   │       ├── RouteStopRepository.java
    │   │               │   │       ├── Vehicle.java
    │   │               │   │       ├── VehicleRepository.java
    │   │               │   │       ├── Driver.java
    │   │               │   │       ├── DriverRepository.java
    │   │               │   │       ├── StudentRouteMapping.java
    │   │               │   │       ├── StudentRouteMappingRepository.java
    │   │               │   │       ├── GpsTrackingEvent.java
    │   │               │   │       └── GpsTrackingEventRepository.java
    │   │               │   │
    │   │               │   ├── admissions/
    │   │               │   │   ├── controller/
    │   │               │   │   │   ├── ApplicationController.java
    │   │               │   │   │   └── MeritListController.java
    │   │               │   │   ├── service/
    │   │               │   │   │   ├── ApplicationWorkflowService.java
    │   │               │   │   │   ├── MeritProcessingService.java
    │   │               │   │   │   └── EnrollmentConversionService.java
    │   │               │   │   ├── dto/
    │   │               │   │   │   ├── ApplicationDto.java
    │   │               │   │   │   ├── MeritListDto.java
    │   │               │   │   │   └── SeatAllotmentDto.java
    │   │               │   │   └── domain/
    │   │               │   │       ├── Application.java
    │   │               │   │       ├── ApplicationRepository.java
    │   │               │   │       ├── ApplicationDocument.java
    │   │               │   │       ├── ApplicationDocumentRepository.java
    │   │               │   │       ├── ApplicationWorkflowStep.java
    │   │               │   │       ├── ApplicationWorkflowStepRepository.java
    │   │               │   │       ├── MeritList.java
    │   │               │   │       ├── MeritListRepository.java
    │   │               │   │       ├── SeatAllotment.java
    │   │               │   │       ├── SeatAllotmentRepository.java
    │   │               │   │       ├── LateralEntryApplication.java
    │   │               │   │       └── LateralEntryApplicationRepository.java
    │   │               │   │
    │   │               │   ├── communication/
    │   │               │   │   ├── controller/
    │   │               │   │   │   ├── NoticeController.java
    │   │               │   │   │   └── HelpDeskController.java
    │   │               │   │   ├── service/
    │   │               │   │   │   ├── NoticeService.java
    │   │               │   │   │   └── HelpDeskService.java
    │   │               │   │   ├── dto/
    │   │               │   │   │   ├── NoticeDto.java
    │   │               │   │   │   ├── CircularDto.java
    │   │               │   │   │   └── HelpDeskTicketDto.java
    │   │               │   │   └── domain/
    │   │               │   │       ├── Notice.java
    │   │               │   │       ├── NoticeRepository.java
    │   │               │   │       ├── NoticeTarget.java
    │   │               │   │       ├── Circular.java
    │   │               │   │       ├── CircularRepository.java
    │   │               │   │       ├── HelpDeskTicket.java
    │   │               │   │       ├── HelpDeskTicketRepository.java
    │   │               │   │       ├── TicketComment.java
    │   │               │   │       └── TicketCommentRepository.java
    │   │               │   │
    │   │               │   ├── placement/
    │   │               │   │   ├── controller/
    │   │               │   │   │   └── PlacementController.java
    │   │               │   │   ├── service/
    │   │               │   │   │   ├── PlacementDriveService.java
    │   │               │   │   │   └── PlacementAnalyticsService.java
    │   │               │   │   ├── dto/
    │   │               │   │   │   ├── CompanyDto.java
    │   │               │   │   │   ├── PlacementDriveDto.java
    │   │               │   │   │   └── PlacementOfferDto.java
    │   │               │   │   └── domain/
    │   │               │   │       ├── Company.java
    │   │               │   │       ├── CompanyRepository.java
    │   │               │   │       ├── PlacementDrive.java
    │   │               │   │       ├── PlacementDriveRepository.java
    │   │               │   │       ├── DriveApplication.java
    │   │               │   │       ├── DriveApplicationRepository.java
    │   │               │   │       ├── InterviewRound.java
    │   │               │   │       ├── InterviewRoundRepository.java
    │   │               │   │       ├── PlacementOffer.java
    │   │               │   │       └── PlacementOfferRepository.java
    │   │               │   │
    │   │               │   └── admin/
    │   │               │       ├── controller/
    │   │               │       │   ├── AuditLogController.java
    │   │               │       │   ├── UserManagementController.java
    │   │               │       │   ├── ReportController.java
    │   │               │       │   ├── SystemHealthController.java
    │   │               │       │   └── NotificationController.java  ← SSE + inbox
    │   │               │       ├── service/
    │   │               │       │   ├── UserManagementService.java
    │   │               │       │   └── ReportBuilderService.java
    │   │               │       └── dto/
    │   │               │           ├── UserDto.java
    │   │               │           ├── CreateUserDto.java
    │   │               │           └── ReportRequestDto.java
    │   │               │
    │   │               └── infrastructure/
    │   │                   └── file/
    │   │                       └── FileController.java             ← Serve stored files
    │   │
    │   └── resources/
    │       ├── application.yml                                     ← Base config (shared defaults)
    │       ├── application-dev.yml                                 ← Dev profile overrides
    │       ├── application-prod.yml                                ← Prod profile overrides
    │       ├── logback-spring.xml                                  ← Structured logging config
    │       └── db/
    │           └── migration/
    │               ├── V001__create_user_tables.sql
    │               ├── V002__create_audit_log.sql
    │               ├── V003__create_academic_tables.sql
    │               ├── V004__create_student_tables.sql
    │               ├── V005__create_attendance_tables.sql
    │               ├── V006__create_examination_tables.sql
    │               ├── V007__create_finance_tables.sql
    │               ├── V008__create_library_tables.sql
    │               ├── V009__create_asset_tables.sql
    │               ├── V010__create_notification_and_scheduler_tables.sql
    │               ├── V011__create_hr_tables.sql
    │               ├── V012__create_hostel_and_transport.sql
    │               ├── V013__create_communication_tables.sql
    │               └── V014__seed_initial_data.sql
    │
    └── test/
        └── java/
            └── com/
                └── college/
                    └── erp/
                        ├── ErpApplicationTest.java                 ← Context loads smoke test
                        ├── module/
                        │   ├── academic/
                        │   │   ├── DepartmentServiceTest.java
                        │   │   └── TimetableConflictTest.java
                        │   ├── student/
                        │   │   └── EnrollmentServiceTest.java
                        │   ├── attendance/
                        │   │   └── AttendanceRulesEngineTest.java
                        │   ├── examination/
                        │   │   ├── MarksEntryServiceTest.java
                        │   │   └── ResultProcessingTest.java
                        │   ├── finance/
                        │   │   ├── FeeCollectionServiceTest.java
                        │   │   ├── LedgerServiceTest.java
                        │   │   ├── SegregationOfDutiesTest.java
                        │   │   └── FinancialYearLockTest.java
                        │   ├── library/
                        │   │   ├── CirculationServiceTest.java
                        │   │   ├── CustodyLogTest.java
                        │   │   └── FineAccrualTest.java
                        │   └── asset/
                        │       └── AssetTransferServiceTest.java
                        └── security/
                            ├── JwtTokenProviderTest.java
                            └── PermissionEvaluatorTest.java
```

---

## 11. Complete Frontend Folder Structure

```
college-erp-frontend/
│
├── index.html                          ← Vite entry; loads env-config.js then main.jsx
├── vite.config.js                      ← Build config: @/ alias, chunk splitting
├── package.json                        ← Dependencies pinned; pnpm preferred
├── .eslintrc.cjs                       ← ESLint rules (no-console, hooks rules)
├── .prettierrc                         ← Auto-formatting config
│
└── src/
    ├── main.jsx                        ← ReactDOM.createRoot → <App />
    ├── App.jsx                         ← React Router root + shell selection
    ├── env.js                          ← Typed accessors for window.__ENV__
    │
    ├── api/                            ← HTTP layer — one file per backend module
    │   ├── client.js                   ← Axios instance: JWT, refresh, error normalize
    │   ├── auth.api.js                 ← login, logout, refresh, changePassword
    │   ├── academic.api.js             ← departments, courses, batches, sections, subjects
    │   ├── student.api.js              ← students, enrollment, guardians
    │   ├── attendance.api.js           ← sessions, mark, submit, summary, condonation
    │   ├── examination.api.js          ← exams, marks, results, hall-tickets
    │   ├── finance.api.js              ← fee-structure, invoices, payments, ledger, grants
    │   ├── library.api.js              ← catalog, circulation, reservations, fines
    │   ├── asset.api.js                ← assets, transfers, maintenance tickets
    │   ├── hr.api.js                   ← staff, leave, appraisals
    │   ├── hostel.api.js               ← rooms, allocations, outpass
    │   ├── transport.api.js            ← routes, vehicles, mappings
    │   ├── communication.api.js        ← notices, helpdesk
    │   ├── notification.api.js         ← inbox, unread count, mark read
    │   └── admin.api.js                ← users, roles, audit log, reports
    │
    ├── hooks/                          ← TanStack Query wrappers (server state)
    │   ├── useStudentProfile.js
    │   ├── useStudentList.js
    │   ├── useAttendanceSummary.js
    │   ├── useAttendanceSession.js
    │   ├── useFeeInvoices.js
    │   ├── useIssuedBooks.js
    │   ├── useNotifications.js
    │   ├── useLeaveBalance.js
    │   ├── useDepartments.js
    │   ├── useCourses.js
    │   ├── useBatches.js
    │   └── useAuditLog.js
    │
    ├── store/                          ← Zustand (UI state only — never server state)
    │   ├── authStore.js                ← {user, roles, accessToken, setAuth, clearAuth}
    │   └── uiStore.js                  ← {sidebarOpen, activeFilters, toggleSidebar}
    │
    ├── features/                       ← Feature modules (mirror backend modules)
    │   │
    │   ├── auth/
    │   │   ├── pages/
    │   │   │   ├── LoginPage.jsx
    │   │   │   └── ChangePasswordPage.jsx
    │   │   └── components/
    │   │       ├── LoginForm.jsx
    │   │       └── TotpVerifyModal.jsx
    │   │
    │   ├── dashboard/
    │   │   └── pages/
    │   │       ├── AdminDashboard.jsx
    │   │       ├── FacultyDashboard.jsx
    │   │       └── StudentDashboard.jsx
    │   │
    │   ├── academic/
    │   │   ├── pages/
    │   │   │   ├── DepartmentListPage.jsx
    │   │   │   ├── DepartmentDetailPage.jsx
    │   │   │   ├── CourseListPage.jsx
    │   │   │   ├── BatchListPage.jsx
    │   │   │   └── SubjectCatalogPage.jsx
    │   │   └── components/
    │   │       ├── DepartmentCard.jsx
    │   │       ├── CourseForm.jsx
    │   │       └── BatchPromoteButton.jsx
    │   │
    │   ├── student/
    │   │   ├── pages/
    │   │   │   ├── StudentListPage.jsx
    │   │   │   ├── StudentDetailPage.jsx
    │   │   │   ├── StudentEnrollPage.jsx
    │   │   │   └── StudentSelfServicePage.jsx
    │   │   └── components/
    │   │       ├── StudentCard.jsx
    │   │       ├── EnrollmentHistoryTimeline.jsx
    │   │       └── SectionTransferModal.jsx
    │   │
    │   ├── attendance/
    │   │   ├── pages/
    │   │   │   ├── AttendanceSessionPage.jsx  ← Faculty marks attendance here
    │   │   │   ├── AttendanceSummaryPage.jsx
    │   │   │   └── DefaulterListPage.jsx
    │   │   └── components/
    │   │       ├── AttendanceGrid.jsx          ← Student × status matrix
    │   │       ├── SessionStatusBadge.jsx
    │   │       └── AttendancePieChart.jsx
    │   │
    │   ├── examination/
    │   │   ├── pages/
    │   │   │   ├── ExamSchedulePage.jsx
    │   │   │   ├── MarksEntryPage.jsx
    │   │   │   ├── ResultPublishPage.jsx
    │   │   │   └── GradeCardPage.jsx
    │   │   └── components/
    │   │       ├── MarksEntryGrid.jsx
    │   │       ├── ResultSummaryCard.jsx
    │   │       └── HallTicketDownloadButton.jsx
    │   │
    │   ├── finance/
    │   │   ├── pages/
    │   │   │   ├── FeeStructurePage.jsx
    │   │   │   ├── FeeCollectionPage.jsx     ← Barcode scan + collect at counter
    │   │   │   ├── InvoiceListPage.jsx
    │   │   │   ├── DefaulterListPage.jsx
    │   │   │   ├── LedgerPage.jsx
    │   │   │   ├── FinancialReportsPage.jsx
    │   │   │   └── AccountantGrantsPage.jsx
    │   │   └── components/
    │   │       ├── InvoiceCard.jsx
    │   │       ├── PaymentModal.jsx
    │   │       ├── ReceiptPrintButton.jsx
    │   │       └── LedgerEntryRow.jsx
    │   │
    │   ├── library/
    │   │   ├── pages/
    │   │   │   ├── CatalogPage.jsx
    │   │   │   ├── BookDetailPage.jsx
    │   │   │   ├── CirculationDeskPage.jsx   ← Issue/Return via barcode scan
    │   │   │   ├── CustodyChainPage.jsx
    │   │   │   ├── FineManagementPage.jsx
    │   │   │   └── StockVerificationPage.jsx
    │   │   └── components/
    │   │       ├── BarcodeScanInput.jsx      ← Auto-focus, debounced scanner input
    │   │       ├── BookCopyCard.jsx
    │   │       ├── CustodyTimeline.jsx
    │   │       └── IssueSummaryCard.jsx
    │   │
    │   ├── asset/
    │   │   ├── pages/
    │   │   │   ├── AssetRegistryPage.jsx
    │   │   │   ├── AssetDetailPage.jsx
    │   │   │   ├── AssetTransferPage.jsx
    │   │   │   ├── CustodyChainPage.jsx
    │   │   │   └── MaintenanceTicketsPage.jsx
    │   │   └── components/
    │   │       ├── AssetCard.jsx
    │   │       ├── AssetTagBadge.jsx
    │   │       └── MaintenanceTicketForm.jsx
    │   │
    │   ├── hr/
    │   │   ├── pages/
    │   │   │   ├── StaffDirectoryPage.jsx
    │   │   │   ├── LeaveManagementPage.jsx
    │   │   │   └── AppraisalPage.jsx
    │   │   └── components/
    │   │       ├── LeaveRequestForm.jsx
    │   │       ├── LeaveBalanceCard.jsx
    │   │       └── LeaveApprovalRow.jsx
    │   │
    │   ├── hostel/
    │   │   └── pages/
    │   │       ├── RoomAllocationPage.jsx
    │   │       └── OutpassPage.jsx
    │   │
    │   ├── transport/
    │   │   └── pages/
    │   │       └── TransportManagementPage.jsx
    │   │
    │   ├── communication/
    │   │   └── pages/
    │   │       ├── NoticeBoardPage.jsx
    │   │       └── HelpDeskPage.jsx
    │   │
    │   ├── placement/
    │   │   └── pages/
    │   │       └── PlacementDrivesPage.jsx
    │   │
    │   └── admin/
    │       ├── pages/
    │       │   ├── UserManagementPage.jsx
    │       │   ├── AuditLogPage.jsx
    │       │   └── ReportBuilderPage.jsx
    │       └── components/
    │           ├── UserRoleAssignModal.jsx
    │           └── AuditLogTable.jsx
    │
    ├── shared/                             ← Reusable across ALL features (DRY)
    │   ├── components/
    │   │   ├── DataTable.jsx               ← Server-paginated table: sort, select, export
    │   │   ├── FormBuilder.jsx             ← Schema-driven form renderer
    │   │   ├── BarcodeScanner.jsx          ← USB HID + camera scanner input
    │   │   ├── PermissionGuard.jsx         ← Route-level auth gate
    │   │   ├── NotificationBell.jsx        ← SSE-connected; shows unread count
    │   │   ├── FileViewer.jsx              ← In-app PDF/image viewer
    │   │   ├── AuditTimeline.jsx           ← Custody chain / audit log viewer
    │   │   ├── PageHeader.jsx              ← Page title + breadcrumb + action buttons
    │   │   ├── StatusBadge.jsx             ← Color-coded status chip
    │   │   ├── ConfirmDialog.jsx           ← Reusable confirmation modal
    │   │   ├── EmptyState.jsx              ← Empty list placeholder
    │   │   └── LoadingSpinner.jsx
    │   ├── hooks/
    │   │   ├── usePermission.js            ← can('PERMISSION.KEY') — reads from JWT claims
    │   │   ├── useCurrentUser.js           ← Auth store selector
    │   │   ├── useDebounce.js              ← Debounce value change (barcode scanner)
    │   │   ├── usePaginationParams.js      ← Read/write page+size from URL params
    │   │   └── useSSE.js                   ← SSE connection + reconnect logic
    │   └── utils/
    │       ├── dateUtils.js                ← formatDate, formatDateTime, timeAgo
    │       ├── currencyUtils.js            ← formatCurrency (₹ symbol, 2dp)
    │       ├── validationUtils.js          ← Email, phone, enrollment number validators
    │       └── permissionConstants.js      ← Permission key constants (avoid magic strings)
    │
    ├── layouts/                            ← Shell layouts — one per role group
    │   ├── AdminShell.jsx                  ← SUPER_ADMIN, ADMIN, IT_ADMIN
    │   ├── AcademicShell.jsx               ← HOD, FACULTY, LAB_ASSISTANT, EXAM_CELL
    │   ├── FinanceShell.jsx                ← HEAD_ACCOUNTANT, ACCOUNTANT, AUDITOR
    │   ├── LibraryShell.jsx                ← CHIEF_LIBRARIAN, LIBRARY_ASSISTANT
    │   ├── AssetShell.jsx                  ← ASSET_MANAGER, MAINTENANCE_STAFF
    │   ├── HRShell.jsx                     ← HR_MANAGER, PLACEMENT_OFFICER
    │   ├── SupportShell.jsx                ← HOSTEL_WARDEN, TRANSPORT_MANAGER
    │   └── StudentShell.jsx                ← STUDENT, PARENT, ALUMNI
    │
    └── router/
        └── index.jsx                       ← React Router v6: lazy routes + permission guards
```

---

## 12. Complete Database Migration Structure

```
src/main/resources/db/migration/
├── V001__create_user_tables.sql            ← users, role_assignments, permission_grants, refresh_tokens
├── V002__create_audit_log.sql              ← audit_log + immutability triggers
├── V003__create_academic_tables.sql        ← departments, academic_years, courses, terms,
│                                              batches, sections, subjects, subject_offerings,
│                                              faculty_subject_mappings
├── V004__create_student_tables.sql         ← students, enrollments (append-only design),
│                                              guardians, student_documents
├── V005__create_attendance_tables.sql      ← attendance_policies, attendance_sessions,
│                                              attendance_records, condonation_requests
├── V006__create_examination_tables.sql     ← exams, hall_tickets, marks_entries,
│                                              result_records, revaluation_requests
├── V007__create_finance_tables.sql         ← financial_years, account_heads, fee_heads,
│                                              fee_structures, fee_invoices, fee_payments,
│                                              ledger_entries (append-only + triggers),
│                                              accountant_permission_grants, vendors, purchase_orders
├── V008__create_library_tables.sql         ← loan_policies, book_titles, book_copies,
│                                              library_custody_log (partial immutability trigger),
│                                              reservations, library_fines
├── V009__create_asset_tables.sql           ← asset_categories, assets,
│                                              asset_custody_log (full immutability trigger),
│                                              maintenance_tickets
├── V010__create_notification_and_scheduler_tables.sql
│                                           ← notification_templates, notification_records,
│                                              background_jobs
├── V011__create_hr_tables.sql              ← designations, staff_profiles, leave_types,
│                                              leave_balances, leave_requests
├── V012__create_hostel_and_transport.sql   ← hostel_blocks, rooms, room_allocations,
│                                              outpass_requests, routes, vehicles,
│                                              student_route_mappings
├── V013__create_communication_tables.sql   ← notices, helpdesk_tickets, ticket_comments
└── V014__seed_initial_data.sql             ← Account heads, fee heads, leave types,
                                               loan policies, asset categories
```

---

## 13. Infrastructure and DevOps

### 13.1 CI/CD Pipeline

```
Every commit:
  1. backend-ci     → mvn compile, unit tests, ArchUnit, Checkstyle
  2. frontend-ci    → pnpm lint, pnpm test, axe accessibility checks
  3. openapi-sync   → regenerate types from spec; fail if drift detected

On merge to main:
  4. integration    → Testcontainers (real PostgreSQL) + all modules
  5. docker-build   → build + tag backend + frontend Docker images
  6. deploy-staging → docker compose up with each profile (cloud, onprem, airgap)
  7. smoke-tests    → Playwright against each profile
  8. airgap-net-check → verify zero outbound HTTP in airgap profile

Nightly:
  9. full-e2e       → Playwright all 15 critical user journeys
 10. dep-audit      → OWASP + npm audit for vulnerabilities
```

### 13.2 Environment Variables (Complete Reference)

| Variable | Default | Profile | Description |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | — | All | `dev`, `prod`, `airgap` |
| `DB_URL` | `jdbc:postgresql://localhost:5432/college_erp` | All | PostgreSQL JDBC URL |
| `DB_USERNAME` | `erp_user` | All | DB username |
| `DB_PASSWORD` | `erp_pass` | All | DB password |
| `JWT_SECRET` | *(dev key)* | All | HS256 secret ≥ 256 bits |
| `JWT_ACCESS_EXPIRY` | `900000` | All | Access token TTL (ms) |
| `JWT_REFRESH_EXPIRY` | `604800000` | All | Refresh token TTL (ms) |
| `STORAGE_MODE` | `local` | All | `local` \| `minio` \| `s3` |
| `STORAGE_LOCAL_PATH` | `./uploads` | All | Local file storage path |
| `MINIO_ENDPOINT` | — | On-prem | MinIO server URL |
| `MINIO_ACCESS_KEY` | — | On-prem | MinIO credentials |
| `MINIO_SECRET_KEY` | — | On-prem | MinIO credentials |
| `MINIO_BUCKET` | `college-erp` | On-prem | MinIO bucket name |
| `S3_BUCKET` | — | Cloud | AWS S3 bucket name |
| `AWS_REGION` | `ap-south-1` | Cloud | AWS region |
| `MAIL_HOST` | `localhost` | All | SMTP host |
| `MAIL_PORT` | `25` | All | SMTP port |
| `NOTIFICATION_EMAIL_ENABLED` | `false` | All | Enable email channel |
| `NOTIFICATION_SMS_ENABLED` | `false` | All | Enable SMS channel |
| `TOTP_ENABLED` | `false` | All | Enforce 2FA |
| `SERVER_PORT` | `8080` | All | HTTP port |
| `LOG_FILE` | `logs/college-erp.log` | All | Log file path |
| `CORS_ORIGINS` | `http://localhost:3000,...` | All | Allowed CORS origins |

---

## 14. Cross-Cutting Concerns

### 14.1 Request Lifecycle

```
Incoming HTTP Request
    │
    ▼ [RequestIdFilter]
    Assigns/reads X-Request-Id header
    Puts requestId + userId in MDC (available in all log lines)
    │
    ▼ [JwtAuthenticationFilter]
    Validates Bearer token
    Sets SecurityContext
    │
    ▼ [TenantFilter] (cloud only)
    Sets app.current_tenant PostgreSQL session variable
    │
    ▼ [Controller]
    @PreAuthorize permission check
    Parse request
    │
    ▼ [Service]
    Business logic
    Repository calls
    Audit log write (async)
    Notification dispatch (async)
    │
    ▼ [GlobalExceptionHandler] (if exception thrown)
    Map to RFC 7807 error response
    Log at appropriate level
    │
    ▼ Outgoing HTTP Response
    X-Request-Id echoed back
    Security headers added by Nginx
```

### 14.2 Audit Log Coverage Map

| Operation | AuditAction | Module |
|---|---|---|
| User login | `LOGIN` | AUTH |
| Failed login | `LOGIN_FAILED` | AUTH |
| Password change | `PASSWORD_CHANGED` | AUTH |
| Account lock | `ACCOUNT_LOCKED` | AUTH |
| Any entity create | `CREATE` | All |
| Any entity update | `UPDATE` | All |
| Soft delete | `SOFT_DELETE` | All |
| Fee collected | `FEE_COLLECTED` | FINANCE |
| Ledger posted | `LEDGER_ENTRY_POSTED` | FINANCE |
| Financial year closed | `FINANCIAL_YEAR_CLOSED` | FINANCE |
| Permission grant created | `PERMISSION_GRANT_CREATED` | AUTH/FINANCE |
| Permission grant revoked | `PERMISSION_GRANT_REVOKED` | AUTH/FINANCE |
| Book issued | `BOOK_ISSUED` | LIBRARY |
| Book returned | `BOOK_RETURNED` | LIBRARY |
| Fine waived | `FINE_WAIVED` | LIBRARY |
| Book marked lost | `BOOK_MARKED_LOST` | LIBRARY |
| Asset transferred | `ASSET_TRANSFERRED` | ASSET |
| Asset disposed | `ASSET_DISPOSED` | ASSET |
| Marks submitted | `MARKS_SUBMITTED` | EXAMINATION |
| Result published | `RESULT_PUBLISHED` | EXAMINATION |

---

## 15. Architecture Decision Records

### ADR-001: Modular Monolith over Microservices

**Status:** Accepted  
**Decision:** Single deployable Spring Boot application, internally modular  
**Reason:** Air-gap requirement makes microservices impractical; ACID transactions needed across modules; single institution's load does not require distributed processing; module boundaries preserved for future extraction if needed

---

### ADR-002: PostgreSQL as the Only Supported Database

**Status:** Accepted  
**Decision:** PostgreSQL 15+ is the only supported database engine  
**Reason:** Free (no licensing server to phone home); mature offline installer; JSONB, full-text search, RLS, triggers all available natively; supporting multiple engines would double the migration/query/testing surface

---

### ADR-003: DB-Backed Job Queue over Message Broker

**Status:** Accepted  
**Decision:** `background_jobs` PostgreSQL table with `SELECT ... FOR UPDATE SKIP LOCKED`  
**Reason:** RabbitMQ/Kafka require extra infrastructure that cannot be assumed on air-gapped networks; DB-backed queue provides durability via PostgreSQL WAL; `SKIP LOCKED` makes it safe for concurrent consumers; simpler to monitor (just a table)

---

### ADR-004: JWT without Embedded Roles

**Status:** Accepted  
**Decision:** JWT contains user ID, username, email — NOT roles or permissions  
**Reason:** Roles and permission grants are loaded fresh from DB on each request; this ensures that a role change or grant revocation takes effect within 15 minutes (one access token lifetime) without forcing a logout; embedding roles in JWT would require token invalidation infrastructure to propagate role changes

---

### ADR-005: Append-Only Enforcement via DB Triggers

**Status:** Accepted  
**Decision:** `audit_log`, `ledger_entries`, `asset_custody_log`, `library_custody_log` enforced immutable by PostgreSQL triggers at the database level  
**Reason:** Application-level enforcement is bypassable (a bug, a direct DB connection, a future developer who doesn't know the rule). DB-trigger enforcement is unconditional regardless of how the data is accessed. This is critical for financial and audit data.

---

### ADR-006: Caffeine as Default Cache (not Redis)

**Status:** Accepted  
**Decision:** Caffeine in-process cache by default; Redis optional  
**Reason:** Caffeine requires zero extra infrastructure — works in all three deployment modes including air-gap. Redis requires a separate service, network configuration, and operational expertise. The cache hit-rate benefit of Redis (cross-instance cache coherence) is only needed in cloud multi-instance deployments. Single-instance on-prem/air-gap deployments get full benefit from Caffeine.

---

*Document Owner: Architecture Team*  
*Last Updated: June 2026*  
*Maintained in version control alongside the codebase*
