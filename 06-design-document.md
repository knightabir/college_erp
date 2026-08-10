# College ERP System — Design Document

**Version:** 1.0  
**Date:** June 2026  
**Status:** Approved  
**Audience:** Engineering Team

---

## Table of Contents

1. [System Architecture](#1-system-architecture)
2. [Backend Design](#2-backend-design)
3. [Frontend Design](#3-frontend-design)
4. [Security Design](#4-security-design)
5. [Data Flow Diagrams](#5-data-flow-diagrams)
6. [Module Interaction Design](#6-module-interaction-design)
7. [Background Job Design](#7-background-job-design)
8. [Notification System Design](#8-notification-system-design)
9. [File Storage Design](#9-file-storage-design)
10. [Deployment Design](#10-deployment-design)
11. [Error Handling Design](#11-error-handling-design)
12. [Caching Design](#12-caching-design)
13. [UI/UX Design Principles](#13-uiux-design-principles)

---

## 1. System Architecture

### 1.1 Architecture Style

The system follows a **Modular Monolith** architecture. It is a single deployable Spring Boot application internally organized into self-contained modules. Each module owns its domain entities, repositories, and services. Modules communicate through well-defined service interfaces — never through direct cross-module database access.

```
┌─────────────────────────────────────────────────────────────────┐
│                    PRESENTATION TIER                             │
│  React 18 SPA (Vite build → static assets served by Nginx)      │
│  All fonts, icons, UI assets bundled at build time               │
└───────────────────────────┬─────────────────────────────────────┘
                            │ HTTPS  /api/v1/*  (REST+JSON)
                            │ Authorization: Bearer <JWT>
┌───────────────────────────▼─────────────────────────────────────┐
│                  APPLICATION TIER                                │
│                Spring Boot 3.x — Single JAR                      │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              Platform Kernel (shared)                     │   │
│  │  Auth · RBAC · Audit · Notifications · Storage · Common  │   │
│  └──────────────────────────────────────────────────────────┘   │
│  ┌──────────┬──────────┬──────────┬──────────┬─────────────┐   │
│  │ academic │ student  │attendance│ finance  │   library   │   │
│  ├──────────┼──────────┼──────────┼──────────┼─────────────┤   │
│  │  asset   │    hr    │  hostel  │transport │communication│   │
│  ├──────────┼──────────┼──────────┴──────────┴─────────────┤   │
│  │admissions│placement │          scheduler                 │   │
│  └──────────┴──────────┴────────────────────────────────────┘   │
└───────────────────────────┬─────────────────────────────────────┘
                            │ JDBC (HikariCP)
┌───────────────────────────▼─────────────────────────────────────┐
│                    DATA TIER                                     │
│  PostgreSQL 15+  (primary OLTP)                                  │
│  + File Storage  (local filesystem / MinIO / S3)                 │
└─────────────────────────────────────────────────────────────────┘
```

### 1.2 Three-Tier Principle

**Tier 1 — Presentation:** React SPA. Contains zero business logic. Talks to backend exclusively via REST/JSON. All validation is re-performed on the server — frontend validation is for UX only.

**Tier 2 — Application:** Spring Boot monolith. Contains all business logic, security enforcement, and orchestration. Is stateless between requests (JWT-based) — can be horizontally scaled.

**Tier 3 — Data:** PostgreSQL. Is the single source of truth. All state lives here — no in-memory state that survives a restart. Background jobs are persisted in the database, not in memory.

### 1.3 Layer Separation Within Application Tier

```
HTTP Request
    │
    ▼
Controller          ← Thin: parses HTTP, calls service, returns DTO
    │                  Never contains business logic
    │                  Never accesses repository directly
    ▼
Service             ← Business logic lives here ONLY
    │                  Calls repositories within its own module
    │                  Calls other module's service interfaces (never their repos)
    │                  Publishes domain events for cross-module async reactions
    ▼
Repository          ← Spring Data JPA
    │                  Package-private within its module
    │                  Returns JPA entities, not DTOs
    ▼
PostgreSQL
```

---

## 2. Backend Design

### 2.1 Package Structure (top-level)

```
com.college.erp
├── ErpApplication.java             ← @SpringBootApplication entry point
├── config/                         ← All Spring @Configuration classes
│   ├── SecurityConfig.java
│   ├── JwtProperties.java
│   ├── ErpProperties.java
│   ├── CorsConfig.java
│   ├── AsyncConfig.java
│   ├── AuditConfig.java
│   ├── JacksonConfig.java
│   └── OpenApiConfig.java
├── common/                         ← Shared code — no domain logic
│   ├── domain/
│   │   └── BaseEntity.java         ← Abstract base: id, timestamps, soft-delete
│   ├── dto/
│   │   ├── ApiResponse.java        ← Universal success wrapper
│   │   ├── PageResponse.java       ← Paginated list wrapper
│   │   └── ErrorResponse.java      ← RFC 7807 error shape
│   ├── exception/                  ← All custom exceptions (one class per concern)
│   └── util/                       ← DateUtil, PaginationUtil, SlugUtil
├── security/                       ← Auth and RBAC infrastructure
│   ├── jwt/
│   ├── rbac/
│   ├── totp/
│   └── session/
├── audit/                          ← System-wide audit log (written async)
├── notification/                   ← Notification channels and SSE
├── scheduler/                      ← DB-backed job queue and scheduled tasks
├── storage/                        ← File storage abstraction and implementations
├── web/                            ← Servlet filters and global exception handler
│   ├── GlobalExceptionHandler.java
│   └── RequestIdFilter.java
├── module/                         ← All feature modules
│   ├── auth/
│   ├── academic/
│   ├── student/
│   ├── attendance/
│   ├── examination/
│   ├── finance/
│   ├── library/
│   ├── asset/
│   ├── hr/
│   ├── hostel/
│   ├── transport/
│   ├── admissions/
│   ├── communication/
│   ├── placement/
│   └── admin/
└── infrastructure/
    └── file/
        └── FileController.java
```

### 2.2 Module Internal Structure

Every feature module follows the same internal layout:

```
module/{name}/
├── controller/         ← REST controllers (HTTP layer only)
├── service/            ← Business logic (all logic here)
├── dto/                ← Request/response objects for the API layer
└── domain/             ← JPA entities AND their repositories (co-located)
    ├── SomeEntity.java
    └── SomeEntityRepository.java
```

**Why entities and repositories are co-located:**  
A repository belongs exclusively to the entity it manages. Co-location makes it immediately obvious which module owns which table. Access to a module's repository from outside the module is prevented by package visibility rules and ArchUnit tests.

### 2.3 BaseEntity Design

All soft-deletable entities extend `BaseEntity`:

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    public void softDelete() { this.deleted = true; }
}
```

`@EnableJpaAuditing` in `AuditConfig` and a custom `AuditorAware<UUID>` bean populate `createdBy`/`updatedBy` from the Spring Security context automatically.

### 2.4 Cross-Module Communication

Two patterns — chosen based on whether the caller needs an immediate answer:

**Pattern 1: Synchronous Service Call**  
Used when module A needs data from module B within the same request.

```java
// In FinanceService — needs student's current enrollment to validate
// CORRECT: call StudentService interface
private final StudentService studentService;  // injected

public FeeInvoice generateInvoice(...) {
    Enrollment enrollment = studentService.getCurrentEnrollment(studentId);
    // use enrollment data...
}
```

```java
// WRONG: never do this — reaches into another module's repository
// private final EnrollmentRepository enrollmentRepository;
```

**Pattern 2: Domain Events (async, in-process)**  
Used when module A triggers something but does not need to know the result.

```java
// In CirculationService (library module)
applicationEventPublisher.publishEvent(new BookIssuedEvent(this, copy, borrowerUserId, dueAt));

// In NotificationEventListener (notification module)
@EventListener
public void onBookIssued(BookIssuedEvent event) {
    notificationService.send(NotificationRequest.builder()
        .recipientUserId(event.getBorrowerUserId())
        .title("Book Issued")
        .body("Due date: " + event.getDueAt())
        .build());
}
```

The Library module publishes the event. The Notification module listens. Neither knows about the other's internals.

### 2.5 DRY in Services

Utilities written once in `common` and used everywhere:

| Utility | Location | Used by |
|---|---|---|
| `PageResponse.of(Page<T>)` | `common.dto.PageResponse` | Every paginated service |
| `ApiResponse.success(data)` | `common.dto.ApiResponse` | Every controller |
| `AuditLogWriter.write(...)` | `audit.AuditLogWriter` | Every service that modifies data |
| `NotificationService.send(...)` | `notification.NotificationService` | Any module sending a notification |
| `FileStorageService.store(...)` | `storage.FileStorageService` | Library, Asset, Student, Exam modules |
| `BarcodeService.generateBarcode(...)` | `library.service.BarcodeService` | Library (copies) and Asset (tags) |

### 2.6 Repository Best Practices

```java
// Good — derived query method names, fully type-safe
Optional<Student> findByEnrollmentNumberAndDeletedFalse(String number);

// Good — JPQL for complex queries, named parameters
@Query("""
    SELECT s FROM Student s
    JOIN Enrollment e ON e.student.id = s.id
    WHERE e.section.id = :sectionId
    AND e.status = 'ACTIVE'
    AND e.is_current = true
    AND s.deleted = false
    ORDER BY s.lastName, s.firstName
    """)
Page<Student> findBySectionId(@Param("sectionId") UUID sectionId, Pageable pageable);

// Never — raw string SQL in service or controller layer
// Never — unbounded findAll() without pagination on large tables
```

---

## 3. Frontend Design

### 3.1 Application Structure

```
college-erp-frontend/
├── public/                     ← Static assets (favicon, robots.txt)
├── src/
│   ├── main.jsx                ← React entry point
│   ├── App.jsx                 ← Router root
│   ├── env.js                  ← Reads window.__ENV__ (runtime config)
│   │
│   ├── api/                    ← One file per backend module
│   │   ├── client.js           ← Axios instance: JWT injection, retry, error handling
│   │   ├── auth.api.js
│   │   ├── academic.api.js
│   │   ├── student.api.js
│   │   ├── attendance.api.js
│   │   ├── examination.api.js
│   │   ├── finance.api.js
│   │   ├── library.api.js
│   │   ├── asset.api.js
│   │   ├── hr.api.js
│   │   └── notification.api.js
│   │
│   ├── hooks/                  ← TanStack Query wrappers (server state)
│   │   ├── useStudentProfile.js
│   │   ├── useAttendanceSummary.js
│   │   ├── useFeeInvoices.js
│   │   ├── useIssuedBooks.js
│   │   └── ...
│   │
│   ├── store/                  ← Zustand (UI-only state, not server state)
│   │   ├── authStore.js        ← Current user, roles, JWT claims
│   │   └── uiStore.js          ← Sidebar open/close, active filters
│   │
│   ├── features/               ← One folder per backend module
│   │   ├── auth/
│   │   │   ├── pages/
│   │   │   │   └── LoginPage.jsx
│   │   │   └── components/
│   │   │       └── LoginForm.jsx
│   │   ├── academic/
│   │   ├── student/
│   │   ├── attendance/
│   │   ├── examination/
│   │   ├── finance/
│   │   ├── library/
│   │   ├── asset/
│   │   ├── hr/
│   │   ├── hostel/
│   │   ├── transport/
│   │   ├── communication/
│   │   ├── placement/
│   │   └── admin/
│   │
│   ├── shared/                 ← Reusable across all features (DRY)
│   │   ├── components/
│   │   │   ├── DataTable.jsx          ← Server-paginated table
│   │   │   ├── FormBuilder.jsx        ← Schema-driven form renderer
│   │   │   ├── BarcodeScanner.jsx     ← Camera / USB HID barcode input
│   │   │   ├── PermissionGuard.jsx    ← Route-level auth gate
│   │   │   ├── NotificationBell.jsx   ← SSE-connected notification bell
│   │   │   ├── FileViewer.jsx         ← In-app PDF/image viewer (PDF.js)
│   │   │   ├── AuditTimeline.jsx      ← Custody chain / audit log viewer
│   │   │   ├── PageHeader.jsx         ← Consistent page title + breadcrumb
│   │   │   ├── StatusBadge.jsx        ← Color-coded status chips
│   │   │   └── ConfirmDialog.jsx      ← Reusable confirmation modal
│   │   ├── hooks/
│   │   │   ├── usePermission.js       ← Check if current user has a permission
│   │   │   ├── useCurrentUser.js      ← Auth store selector
│   │   │   └── useDebounce.js
│   │   └── utils/
│   │       ├── dateUtils.js           ← Format dates, calculate age, etc.
│   │       ├── currencyUtils.js       ← Format ₹ amounts
│   │       └── validationUtils.js     ← Shared form validation helpers
│   │
│   ├── layouts/                ← Role-aware shell layouts
│   │   ├── AdminShell.jsx
│   │   ├── AcademicShell.jsx
│   │   ├── FinanceShell.jsx
│   │   ├── LibraryShell.jsx
│   │   ├── AssetShell.jsx
│   │   ├── HRShell.jsx
│   │   ├── SupportShell.jsx
│   │   └── StudentShell.jsx
│   │
│   ├── router/
│   │   └── index.jsx           ← React Router v6 routes + lazy loading
│   │
│   └── types/                  ← TypeScript-style JSDoc types (or .ts if TS used)
│       └── generated/          ← Auto-generated from OpenAPI spec
│
├── index.html
├── vite.config.js
└── package.json
```

### 3.2 State Management

**Two concerns, two tools — never mixed:**

| Concern | Tool | Example |
|---|---|---|
| Server state (data from API) | TanStack Query | Student list, fee invoices, attendance % |
| UI state (local to browser session) | Zustand | Sidebar open, active filter values, modal open |

**Why not Redux:** Redux is designed for complex client-side state machines. An ERP's client side is primarily a data display and form submission tool — TanStack Query handles 90% of the state management need without any Redux boilerplate.

### 3.3 API Client Design (DRY)

A single `api/client.js` handles all cross-cutting concerns. Individual module API files never duplicate these:

```javascript
// api/client.js — written once, used by all api/*.api.js files

const client = axios.create({ baseURL: window.__ENV__.API_BASE_URL });

// JWT injection — runs on every outgoing request
client.interceptors.request.use(config => {
    const token = useAuthStore.getState().accessToken;
    if (token) config.headers.Authorization = `Bearer ${token}`;
    return config;
});

// Silent token refresh — runs on every 401 response
client.interceptors.response.use(null, async error => {
    if (error.response?.status === 401 && !error.config._retry) {
        error.config._retry = true;
        const newToken = await refreshAccessToken();
        error.config.headers.Authorization = `Bearer ${newToken}`;
        return client(error.config);
    }
    return Promise.reject(normalizeError(error));
});

// RFC 7807 error normalization — consistent shape for all errors
function normalizeError(error) {
    const data = error.response?.data;
    return {
        title: data?.title ?? 'Request Failed',
        detail: data?.detail ?? error.message,
        status: data?.status ?? 0,
        requestId: data?.request_id,
        fieldErrors: data?.field_errors ?? {}
    };
}
```

### 3.4 Permission Guard Pattern

```jsx
// Used at route level — prevents unauthorized route access
<PermissionGuard require="CHIEF_LIBRARIAN">
    <CatalogManagementPage />
</PermissionGuard>

// Used inline — shows/hides individual UI elements
function CopyCard({ copy }) {
    const { can } = usePermission();
    return (
        <Card>
            <p>{copy.barcode}</p>
            {can('LIBRARY.FINE_WAIVER') && (
                <WaiveFineButton copyId={copy.id} />
            )}
        </Card>
    );
}
```

`usePermission()` reads from the JWT claims stored in Zustand auth store — no server call on every permission check.

### 3.5 Shell Layout System

Each role group gets a purpose-built shell that shows only the navigation relevant to their role. This is better than one universal sidebar with conditional hiding — it keeps each role's UI clean, reduces JavaScript bundle size via lazy loading, and makes the intent of each module explicit.

```jsx
// App.jsx — route → shell mapping
function App() {
    const { primaryRole } = useCurrentUser();
    const Shell = shellForRole(primaryRole);
    return <Shell />;
}

function shellForRole(role) {
    const map = {
        SUPER_ADMIN: AdminShell,
        ADMIN: AdminShell,
        HEAD_ACCOUNTANT: FinanceShell,
        ACCOUNTANT: FinanceShell,
        CHIEF_LIBRARIAN: LibraryShell,
        LIBRARY_ASSISTANT: LibraryShell,
        HOD: AcademicShell,
        FACULTY: AcademicShell,
        STUDENT: StudentShell,
        PARENT: StudentShell,
        // ...
    };
    return map[role] ?? StudentShell;
}
```

### 3.6 Runtime Environment Injection

Environment values are written at container start — not baked into the JS bundle:

```html
<!-- index.html -->
<script src="/env-config.js"></script>  <!-- written by Nginx entrypoint -->
<script type="module" src="/assets/main.js"></script>
```

```javascript
// /env-config.js (generated by docker-entrypoint.sh)
window.__ENV__ = {
  API_BASE_URL: 'https://erp.college.edu/api/v1',
  DEPLOYMENT_MODE: 'cloud',
  FEATURE_MAP_ENABLED: 'true',
  FEATURE_SMS_ENABLED: 'false',
  TENANT_NAME: 'Springfield College'
};
```

```javascript
// env.js — type-safe accessor
export const env = {
    apiBaseUrl: window.__ENV__.API_BASE_URL,
    deploymentMode: window.__ENV__.DEPLOYMENT_MODE,
    featureMapEnabled: window.__ENV__.FEATURE_MAP_ENABLED === 'true',
    featureSmsEnabled: window.__ENV__.FEATURE_SMS_ENABLED === 'true',
    tenantName: window.__ENV__.TENANT_NAME
};
```

---

## 4. Security Design

### 4.1 Authentication Flow

```
Client                          Server
  │                               │
  │  POST /auth/login             │
  │  {identifier, password}       │
  │──────────────────────────────►│
  │                               │ 1. Load user by username/email
  │                               │ 2. BCrypt verify password
  │                               │ 3. Check account enabled/not locked
  │                               │ 4. Load active role assignments
  │                               │ 5. Generate JWT (15 min expiry)
  │                               │ 6. Generate random refresh token
  │                               │ 7. Store SHA-256(refreshToken) in DB
  │                               │
  │  200 {access_token,           │
  │       refresh_token}          │
  │◄──────────────────────────────│
  │                               │
  │  GET /api/v1/students         │
  │  Authorization: Bearer <JWT>  │
  │──────────────────────────────►│
  │                               │ 1. JwtAuthenticationFilter validates JWT
  │                               │ 2. Extract user_id from JWT claims
  │                               │ 3. Load UserPrincipal from DB
  │                               │ 4. Set SecurityContext
  │                               │ 5. @PreAuthorize evaluates permission
  │                               │ 6. Controller executes
  │◄──────────────────────────────│
```

### 4.2 JWT Structure

```
Header:  { "alg": "HS256" }
Payload: {
    "iss": "college-erp",
    "sub": "550e8400-e29b-41d4-a716-446655440000",  ← user UUID
    "username": "jdoe",
    "email": "jdoe@college.edu",
    "iat": 1717228800,
    "exp": 1717229700,
    "jti": "unique-token-id"
}
Signature: HMAC-SHA256(base64(header) + "." + base64(payload), secret)
```

Roles and permissions are **not** embedded in the JWT — they are loaded fresh from the database on each request. This ensures role changes (e.g., an accountant being locked out) take effect within one access token's lifetime (15 minutes) without requiring a logout.

### 4.3 Permission Evaluation Flow

```
@PreAuthorize("@permissionEvaluator.canCollectFee(principal, #request.departmentId, #request.amount)")
public ReceiptDto collectFee(FeeCollectionRequest request) { ... }

PermissionEvaluator.canCollectFee(principal, departmentId, amount):
    1. Load role templates for user's active roles
    2. If HEAD_ACCOUNTANT role → return TRUE (unconditional full access)
    3. If ACCOUNTANT or FEE_COLLECTION_CLERK role:
        a. Load active permission grants where permission_key = 'FINANCE.FEE_COLLECTION'
        b. Filter grants where scope_department_id IS NULL (unscoped) OR matches departmentId
        c. Filter grants where amount_threshold IS NULL OR amount <= threshold
        d. If any matching grant found → return TRUE
    4. Return FALSE (deny)
```

### 4.4 Sensitive Data Handling

| Data | Storage | Transmission |
|---|---|---|
| Passwords | BCrypt hash only | Never transmitted after creation |
| Refresh tokens | SHA-256 hash only | Raw token sent to client once, then discarded |
| TOTP secrets | Encrypted at rest | Never transmitted |
| Aadhar numbers | Stored in DB | Masked on API responses (e.g., XXXX-XXXX-1234) |
| Bank account numbers | Stored in DB | Write-only from UI; masked on read |
| JWT secret | Environment variable | Never in logs, never in responses |

---

## 5. Data Flow Diagrams

### 5.1 Fee Collection Flow

```
Student/Parent                 System                           DB
     │                           │                              │
     │   Pay fee online          │                              │
     │──────────────────────────►│                              │
     │                           │ 1. Load FeeInvoice           │
     │                           │──────────────────────────────►
     │                           │ 2. Validate: not paid/cancelled
     │                           │ 3. Validate: amount ≤ balance
     │                           │ 4. Create FeePayment record  │
     │                           │──────────────────────────────►
     │                           │ 5. Update invoice.paid_amount│
     │                           │ 6. Update invoice.status     │
     │                           │──────────────────────────────►
     │                           │ 7. Post DEBIT to ledger      │
     │                           │ 8. Post CREDIT to ledger     │
     │                           │──────────────────────────────►
     │                           │ 9. Write audit log entry     │
     │                           │──────────────────────────────►
     │                           │ 10. Queue notification job   │
     │                           │──────────────────────────────►
     │  201 {receipt_number}     │                              │
     │◄──────────────────────────│                              │
     │                           │                              │
     │  [async] SSE notification │                              │
     │◄──────────────────────────│ 11. Job runs, sends SSE push │
```

### 5.2 Library Book Issue Flow

```
Library Staff                  System                           DB
     │                           │                              │
     │   Scan barcode + ID        │                              │
     │──────────────────────────►│                              │
     │                           │ 1. Load BookCopy by barcode  │
     │                           │──────────────────────────────►
     │                           │ 2. Check: status == AVAILABLE│
     │                           │ 3. Load borrower's active issues
     │                           │──────────────────────────────►
     │                           │ 4. Check: no overdue books   │
     │                           │ 5. Check: loan limit not reached
     │                           │ 6. Load LoanPolicy for type  │
     │                           │──────────────────────────────►
     │                           │ 7. Calculate due date        │
     │                           │ 8. INSERT LibraryCustodyLog  │  ← append-only
     │                           │──────────────────────────────►
     │                           │ 9. UPDATE copy.status=ISSUED │
     │                           │ 10. UPDATE title.available-- │
     │                           │──────────────────────────────►
     │                           │ 11. Write audit log          │
     │                           │──────────────────────────────►
     │  201 {custody_log}        │                              │
     │◄──────────────────────────│                              │
```

### 5.3 Attendance Marking Flow

```
Faculty                        System                           DB
     │                           │                              │
     │   Open session            │                              │
     │──────────────────────────►│                              │
     │                           │ 1. Check: no session exists  │
     │                           │    for offering+section+date │
     │                           │──────────────────────────────►
     │                           │ 2. INSERT AttendanceSession  │
     │                           │    (status: DRAFT)           │
     │                           │──────────────────────────────►
     │  201 {session}            │                              │
     │◄──────────────────────────│                              │
     │                           │                              │
     │   Mark attendance         │                              │
     │   {student_id → status}   │                              │
     │──────────────────────────►│                              │
     │                           │ 3. DELETE old records        │
     │                           │ 4. INSERT new records        │
     │                           │──────────────────────────────►
     │  200 OK                   │                              │
     │◄──────────────────────────│                              │
     │                           │                              │
     │   Submit session          │                              │
     │──────────────────────────►│                              │
     │                           │ 5. Check records not empty   │
     │                           │ 6. UPDATE session.status     │
     │                           │    = SUBMITTED               │
     │                           │──────────────────────────────►
     │                           │ 7. Write audit log           │
     │  200 OK                   │──────────────────────────────►
     │◄──────────────────────────│                              │
```

---

## 6. Module Interaction Design

### 6.1 Dependency Graph (direction = "A depends on B")

```
finance ──────────────► student
finance ──────────────► academic
library ──────────────► (none — self-contained)
asset ─────────────────► (none — self-contained)
attendance ───────────► academic, student
examination ──────────► academic, student, attendance (for eligibility)
admissions ───────────► academic, student (creates student on conversion)
hr ───────────────────► (none — self-contained)
hostel ───────────────► student
transport ────────────► student
communication ────────► (none — uses notification platform)
placement ────────────► student, academic

All modules ──────────► platform kernel (auth, audit, notification, storage)
```

**Critical rule:** No cycles. If module A depends on B, module B must never depend on A.

### 6.2 Event Subscriptions

| Event Published By | Event Name | Listener | Action |
|---|---|---|---|
| library.CirculationService | BookIssuedEvent | notification | Send due-date reminder job |
| library.CirculationService | BookReturnedEvent | notification | Notify reservation queue |
| library.CirculationService | BookOverdueEvent | notification | Send overdue alert |
| finance.FeeCollectionService | FeePaymentReceivedEvent | notification | Send receipt notification |
| attendance.AttendanceService | AttendanceShortfallEvent | notification | Alert parent |
| examination.ResultService | ResultPublishedEvent | notification | Notify students |
| student.StudentService | StudentGraduatedEvent | alumni | Create alumni profile |
| admissions.EnrollmentConversionService | StudentEnrolledEvent | academic | Set up subject offerings |

---

## 7. Background Job Design

### 7.1 Job Queue Table

```
background_jobs
├── id (UUID)
├── job_type (VARCHAR)        ← dispatched to the right job handler
├── payload (JSONB)           ← job-specific data (student_id, offering_id, etc.)
├── status                    ← PENDING → RUNNING → DONE / FAILED
├── attempts (INT)
├── max_attempts (INT)
├── scheduled_for (TIMESTAMPTZ)   ← can be future-dated for delayed jobs
├── started_at
├── completed_at
└── last_error
```

### 7.2 Polling Strategy

```
JobDispatcher (runs every 30 seconds):
    BEGIN TRANSACTION
    SELECT * FROM background_jobs
    WHERE status = 'PENDING'
    AND scheduled_for <= NOW()
    ORDER BY scheduled_for ASC
    LIMIT 10
    FOR UPDATE SKIP LOCKED   ← multiple instances, no double-processing
    
    For each job:
        job.status = RUNNING
        job.attempts++
        job.started_at = NOW()
        SAVE
        
        try { dispatch(job.jobType, job.payload) }
        catch { job.markFailed(error) }
        finally { SAVE }
    COMMIT
```

### 7.3 Retry with Exponential Backoff

```
attempt 1 failure → retry in 30s   (2^1 × 30)
attempt 2 failure → retry in 120s  (2^2 × 30)
attempt 3 failure → FAILED         (max_attempts = 3 by default)
```

---

## 8. Notification System Design

### 8.1 Channel Abstraction

```java
public interface NotificationChannel {
    boolean isEnabled();
    void send(NotificationRequest request);
}
```

Three implementations: `InAppChannel`, `EmailChannel`, `SmsChannel`.  
`InAppChannel.isEnabled()` always returns `true`.  
`EmailChannel.isEnabled()` returns `erp.notification.email.enabled` property value.  
`SmsChannel.isEnabled()` returns `erp.notification.sms.enabled` — `false` in air-gap profile.

### 8.2 Delivery Flow

```
Any service calls NotificationService.send(request)
    │
    ├── 1. Persist NotificationRecord in DB (in-app inbox) — always
    │
    ├── 2. Push via SSE if user has open connection — always
    │
    ├── 3. EmailChannel.send() — if enabled
    │       failure here does NOT roll back step 1 or 2
    │
    └── 4. SmsChannel.send() — if enabled
            failure here does NOT roll back steps 1-3
```

Key design: `NotificationService.send()` is annotated `@Async` — it never blocks the calling business transaction. A fee collection completes even if the notification system is temporarily down.

### 8.3 SSE Connection Management

```java
SseEmitterRegistry:
    Map<UUID, SseEmitter> emitters    ← ConcurrentHashMap, one per logged-in user
    
    create(userId) → register emitter, set completion/timeout/error handlers
    push(userId, notification) → find emitter, send event, handle disconnect
    remove(userId) → clean up on disconnect
```

If the user has no open SSE connection (e.g., they're on mobile or the browser tab is closed), the notification is still persisted in `notification_records`. It appears in their inbox on next login.

---

## 9. File Storage Design

### 9.1 Interface-First

```java
public interface FileStorageService {
    StoredFile store(FileUploadRequest request, StorageScope scope);
    InputStream retrieve(String fileId);
    void delete(String fileId);     // soft delete — marks file, does not remove
}
```

### 9.2 Storage Scope

`StorageScope` determines the storage directory/bucket prefix:

```java
public enum StorageScope {
    STUDENT_DOC,      // Student identity documents
    LIBRARY_ASSET,    // Book cover images, barcode PNGs
    ASSET,            // Asset photos, asset barcode PNGs
    GENERATED,        // Grade cards, receipts (PDFs)
    PROFILE_PHOTO,    // User profile photos
    EXAM_PAPER        // Confidential question papers
}
```

### 9.3 Implementation Selection

Selected by `erp.storage.mode` property:

```yaml
# Local filesystem (default, air-gap safe)
erp.storage.mode: local
erp.storage.local.base-path: ./uploads

# MinIO (on-prem with S3 API)
erp.storage.mode: minio
erp.storage.minio.endpoint: http://minio:9000

# AWS S3 (cloud)
erp.storage.mode: s3
erp.storage.s3.bucket: college-erp-prod
```

The `@ConditionalOnProperty` annotation activates the correct implementation. Business code never changes — only configuration changes.

---

## 10. Deployment Design

### 10.1 Docker Compose (Base)

```yaml
services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: college_erp
      POSTGRES_USER: erp_user
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U erp_user"]
      interval: 10s

  backend:
    image: college-erp-backend:${VERSION}
    depends_on:
      postgres: { condition: service_healthy }
    environment:
      SPRING_PROFILES_ACTIVE: ${DEPLOY_MODE}
      DB_URL: jdbc:postgresql://postgres:5432/college_erp
      DB_USERNAME: erp_user
      DB_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
    ports:
      - "8080:8080"

  frontend:
    image: college-erp-frontend:${VERSION}
    depends_on: [backend]
    environment:
      API_BASE_URL: ${API_BASE_URL}
      DEPLOYMENT_MODE: ${DEPLOY_MODE}
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/certs:/etc/nginx/certs:ro

volumes:
  postgres_data:
```

### 10.2 Profile Activation

```bash
# Cloud
DEPLOY_MODE=cloud docker compose up -d

# On-premises
DEPLOY_MODE=onprem docker compose up -d

# Air-gapped
DEPLOY_MODE=airgap docker compose up -d
```

Same images. Same compose file. One environment variable changes everything.

---

## 11. Error Handling Design

### 11.1 Exception Hierarchy

```
Exception
└── RuntimeException
    └── ErpException (carries HttpStatus)
        ├── ResourceNotFoundException     → HTTP 404
        ├── ValidationException           → HTTP 400
        ├── DuplicateResourceException    → HTTP 409
        ├── BusinessRuleViolationException → HTTP 422
        └── AccessDeniedException         → HTTP 403
```

**One exception class per concern** — named by what went wrong, not by HTTP status. Services throw `BusinessRuleViolationException("Cannot collect fee — invoice is already paid")`. The global handler maps it to HTTP 422 and the RFC 7807 response shape.

### 11.2 Global Exception Handler

`GlobalExceptionHandler` (annotated `@RestControllerAdvice`) handles:
1. `ErpException` and subclasses → mapped status + RFC 7807 body
2. `MethodArgumentNotValidException` → 400 with field-level error map
3. `BadCredentialsException` → 401
4. `AuthorizationDeniedException` → 403
5. `Exception` (catch-all) → 500, logs full stack trace server-side, returns generic message to client

**Never expose stack traces to clients.** The `server.error.include-stacktrace=never` property ensures Spring Boot's default error handling also hides traces.

---

## 12. Caching Design

### 12.1 What Is Cached

| Cache Name | Content | TTL | Eviction Trigger |
|---|---|---|---|
| `departments` | Department by ID | 5 min | Create/update/deactivate |
| `active-departments` | All active departments | 5 min | Same as above |
| `courses` | Course by ID | 10 min | Create/update |
| `loan-policies` | Library loan policies | 30 min | Policy update |
| `fee-heads` | All active fee heads | 10 min | Fee head create/update |
| `account-heads` | All account heads | 10 min | Account head create/update |

### 12.2 Cache Implementation

Default: **Caffeine** (in-process, zero infrastructure). Works identically in all three deployment modes.

Optional: **Redis** for multi-instance cloud deployments where cache coherence across instances is needed. Activated by adding `spring-boot-starter-data-redis` and setting `spring.cache.type=redis` in the cloud profile.

```java
@Cacheable(value = "departments", key = "#id")
public Department getById(UUID id) { ... }

@CacheEvict(value = {"departments", "active-departments"}, allEntries = true)
public Department create(Department dept) { ... }
```

---

## 13. UI/UX Design Principles

### 13.1 Role-Specific Simplicity

Each role sees only what they need. A Faculty member's dashboard shows today's classes and attendance sessions. They do not see financial reports or library catalog management. Clutter is the enemy of adoption.

### 13.2 Barcode-First Library UX

The library circulation desk UI is designed for keyboard-free operation via barcode scanner (USB HID):
- Focus automatically moves to the barcode input field on page load
- Pressing Enter after scanning immediately triggers the issue/return action
- The scanner input debounces at 100ms to distinguish scanner input from keyboard typing

### 13.3 Offline Resilience

When the backend is unreachable (rare for campus networks but possible):
- TanStack Query shows stale cached data with a "Last updated X ago" indicator
- Action buttons are disabled when offline
- A dismissible top-bar banner says "You appear to be offline — data may be out of date"

### 13.4 Progressive Disclosure

Long forms (student enrollment, fee structure setup) are broken into multi-step wizards. Each step validates before proceeding. This reduces cognitive load and makes the "where did I go wrong" problem trivial.

### 13.5 Accessibility First

- All form controls have associated `<label>` elements
- Color is never the sole indicator of state (always accompanied by icon or text)
- Focus management: modals trap focus; after close, focus returns to trigger element
- `aria-live` regions announce dynamic updates (e.g., attendance mark count) to screen readers

---

*Document Owner: Architecture Team*  
*Last Updated: June 2026*
