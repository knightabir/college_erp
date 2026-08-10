# College ERP System — Software Requirements Specification (SRS)

**Version:** 1.0  
**Date:** June 2026  
**Status:** Approved  
**Standard:** IEEE 830-1998 (adapted)

---

## Table of Contents

1. [Introduction](#1-introduction)
2. [Overall Description](#2-overall-description)
3. [System Features — Functional Requirements](#3-system-features--functional-requirements)
4. [External Interface Requirements](#4-external-interface-requirements)
5. [Non-Functional Requirements](#5-non-functional-requirements)
6. [Data Requirements](#6-data-requirements)
7. [System Constraints](#7-system-constraints)
8. [Assumptions and Dependencies](#8-assumptions-and-dependencies)
9. [Appendix — Glossary](#9-appendix--glossary)

---

## 1. Introduction

### 1.1 Purpose

This Software Requirements Specification (SRS) defines the complete functional and non-functional requirements for the College ERP System. It is the authoritative reference for the engineering team during design, implementation, and testing phases.

### 1.2 Scope

The system covers:
- Authentication, authorization, and role management
- Academic structure (departments, courses, batches, sections, subjects)
- Student lifecycle management
- Attendance capture and rules enforcement
- Examination, marks entry, and result processing
- Financial accounting (fee management, double-entry ledger, payroll)
- Library catalog and circulation with per-copy custody tracking
- Asset registry and custody management
- HR and leave management
- Communication (notices, helpdesk)
- Background job scheduling
- Audit logging

### 1.3 Definitions, Acronyms, Abbreviations

| Term | Definition |
|---|---|
| RBAC | Role-Based Access Control |
| JWT | JSON Web Token — used for stateless authentication |
| TOTP | Time-based One-Time Password — used for 2FA |
| SRS | Software Requirements Specification |
| FR | Functional Requirement |
| NFR | Non-Functional Requirement |
| HOD | Head of Department |
| DRY | Don't Repeat Yourself — architectural principle |
| SSE | Server-Sent Events — used for real-time in-app notifications |
| Append-only | A table or record that can only be INSERTed, never UPDATEd or DELETEd |
| Air-gap | A network physically isolated from the internet |
| Tenant | A single institution using the system (relevant for cloud multi-tenant mode) |
| Soft delete | Marking a record as deleted (is_deleted=true) without physically removing it |

### 1.4 References

- Project Vision Document v1.0
- Product Requirements Document v1.0
- Roles and Permissions Document v1.0
- Spring Boot 3.x Official Documentation
- PostgreSQL 15 Documentation
- React 18 Official Documentation
- OWASP Application Security Verification Standard (ASVS) Level 2

---

## 2. Overall Description

### 2.1 Product Perspective

The system is a three-tier web application:

```
[Browser / Mobile Browser]
         |  HTTPS (REST+JSON)
[React SPA — served by Nginx]
         |  HTTP proxy /api/*
[Spring Boot Application — single JAR]
         |  JDBC
[PostgreSQL Database]
```

It is designed as a **modular monolith** — one deployable unit internally organized by feature domain. Modules share a common platform kernel (auth, audit, notifications, storage) but do not share database tables or service implementations.

### 2.2 Product Functions (Summary)

1. Authenticate users and enforce role + scope-based access
2. Manage academic hierarchy (department → course → batch → section → subject)
3. Manage student enrollment with immutable history
4. Capture attendance per class session and enforce eligibility rules
5. Schedule exams, capture marks (with moderation workflow), and publish results
6. Collect fees, maintain a double-entry ledger, and enforce scoped accountant access
7. Manage library catalog with per-copy barcode tracking and custody chain
8. Register and track institutional assets with full transfer history
9. Manage staff leave with balance tracking and approval workflow
10. Send notifications via in-app SSE, email, and SMS
11. Log every state change in an immutable, searchable audit trail
12. Run scheduled background jobs (fee reminders, attendance scans, overdue fines)

### 2.3 User Classes and Characteristics

Refer to the PRD User Personas section. System roles are formally defined in the Roles and Permissions document.

### 2.4 Operating Environment

- **Server OS:** Ubuntu 20.04 LTS or 22.04 LTS (primary); RHEL 8+ (supported)
- **Java:** OpenJDK 21 LTS
- **Database:** PostgreSQL 15+
- **Container:** Docker 24+, Docker Compose v2+
- **Browser:** Chrome 100+, Firefox 100+, Edge 100+, Safari 15+
- **Mobile browser:** Chrome for Android, Safari for iOS

### 2.5 Design and Implementation Constraints

1. Backend written exclusively in Java 21 using Spring Boot 3.x
2. Frontend written in JavaScript using React 18 and Vite
3. Database is PostgreSQL — no other database engine is supported
4. All schema changes go through Flyway migrations — no DDL outside migrations
5. No business logic in controllers — thin controller, thick service pattern enforced
6. No direct SQL joins across module-owned tables — cross-module data access via service interfaces only
7. All shared utilities are in the `common` package — no duplication across modules
8. Frontend assets must be fully self-contained at build time — no CDN or external resource at runtime

---

## 3. System Features — Functional Requirements

> Each requirement uses the notation: **[MODULE-SEQ] Title: Requirement text**

---

### 3.1 Authentication Module

**[AUTH-001] Local Login:**  
The system shall authenticate users by username or email address combined with a bcrypt-verified password. On success it shall return a JWT access token and a cryptographically random refresh token.

**[AUTH-002] JWT Access Token:**  
The access token shall be a signed HS256 JWT containing: user ID (sub), username, email, and issuer claims. The token shall expire after a configurable duration (default: 15 minutes).

**[AUTH-003] Refresh Token Rotation:**  
The refresh token shall be stored in the database as a SHA-256 hash. The raw token is never stored. On use, the system shall validate the token is non-expired and non-revoked and issue a new access token. Refresh tokens expire after a configurable duration (default: 7 days).

**[AUTH-004] Account Lockout:**  
After N failed login attempts (default: 5, configurable), the account shall be locked for M minutes (default: 30, configurable). The lockout is automatically lifted after the duration expires.

**[AUTH-005] TOTP Two-Factor Authentication:**  
The system shall support RFC 6238 TOTP-based 2FA. The TOTP secret is stored encrypted. No SMS is used — TOTP is validated locally. When enabled for an account, login requires both password and current TOTP code.

**[AUTH-006] Password Change:**  
Authenticated users shall be able to change their password by providing their current password plus a new password with confirmation. On change, all active refresh tokens for the user shall be revoked.

**[AUTH-007] Session Management:**  
The system shall maintain a list of active refresh tokens per user. Admins shall be able to revoke all sessions for any user. Users shall be able to revoke their own sessions.

**[AUTH-008] Forced Password Change:**  
Users with `must_change_password = true` (set on account creation) shall be required to change their password on first login before any other action is permitted.

---

### 3.2 Role-Based Access Control

**[RBAC-001] Role Assignment:**  
Every user shall have one or more role assignments. A role assignment includes: role name (enum), optional department scope, assigned-by user, effective dates. Expired or inactive assignments are ignored.

**[RBAC-002] Conditional Permission Grants:**  
Beyond role templates, a superior role may grant a specific user a specific permission key with optional scope dimensions (department ID, batch ID, fee-head code, amount threshold) and optional expiry. This is used for the Head Accountant → sub-accountant and Chief Librarian → Library Assistant delegation models.

**[RBAC-003] Permission Evaluation:**  
Every protected service method shall evaluate: (a) does the user's role template grant this action? (b) if the permission is conditional, does the user have an active custom grant covering the requested scope? If neither, access is denied.

**[RBAC-004] No Self-Grant:**  
The system shall reject any permission grant where `grantee_user_id == granted_by_user_id`.

**[RBAC-005] Segregation of Duties — Finance:**  
For financial transactions above a configurable amount threshold, the user who creates the transaction shall not be the sole approver. The system shall enforce this at the service layer, not relying on UI controls.

**[RBAC-006] Dual Approval — Year Close:**  
The financial year close operation shall require two separate approval records: one from the Head Accountant and one from the Admin. The close shall not execute until both are present.

**[RBAC-007] Marks Publication:**  
Marks entered by faculty shall have status `SUBMITTED`. Publication (status change to `PUBLISHED`) shall only be executable by HOD or Exam Cell — not by the faculty member who entered them.

---

### 3.3 Academic Management

**[ACAD-001] Department CRUD:**  
The system shall support creating, reading, updating, and soft-deleting departments. Each department has a unique code, name, optional HOD assignment, and contact details. Duplicate codes are rejected with an informative error.

**[ACAD-002] Course Management:**  
Courses belong to a department. Each course has: code (unique system-wide), name, level (UG/PG/DIPLOMA/PHD/CERTIFICATE), duration in years, total credits, and intake capacity.

**[ACAD-003] Batch Management:**  
A batch is created per course per academic year. A batch carries a current term number, which advances when the Admin promotes the batch. The system shall prevent promotion beyond the course's maximum terms (duration_years × 2).

**[ACAD-004] Section Management:**  
Sections (A, B, C, etc.) are created under a batch. Section names must be unique within a batch. Each section has a capacity.

**[ACAD-005] Subject Catalog:**  
Subjects are system-wide (reusable across courses). Each subject has: code (unique), name, owning department, credits, type (THEORY/LAB/PROJECT/ELECTIVE/SEMINAR).

**[ACAD-006] Subject Offerings:**  
A subject offering binds a subject to a batch+term combination, specifying maximum internal marks, maximum external marks, and pass thresholds. The same subject can be offered to multiple batches in the same term.

**[ACAD-007] Faculty Allocation:**  
Faculty are assigned to specific subject offerings per section. A single offering+section may have one primary faculty and additional co-teachers/lab assistants.

---

### 3.4 Student Management

**[STU-001] Student Enrollment:**  
Enrolling a student creates both a student profile and an initial enrollment record linking them to a batch+section+term. The enrollment number must be unique system-wide.

**[STU-002] Enrollment History:**  
The enrollment table is append-only. Every batch, section, or term change creates a new enrollment row with the previous row marked `is_current = false`. The current enrollment is always the row where `is_current = true`.

**[STU-003] Section Transfer:**  
Transferring a student to a new section: marks the current enrollment as `TRANSFERRED`, creates a new enrollment in the new section with `is_current = true`. The full history is preserved.

**[STU-004] Graduation:**  
Marking a student as graduated sets their status to `GRADUATED`, records the graduation date, and marks the current enrollment as inactive.

**[STU-005] Guardian Linkage:**  
Each student may have one or more guardians. A guardian may optionally have a system user account, which gives them access to the Parent portal (read-only student view + fee payment).

**[STU-006] Document Vault:**  
Students may have documents uploaded against their profile (ID proof, certificates). Documents have a verification status (UNVERIFIED, VERIFIED, EXPIRED).

**[STU-007] Self-Service Portal:**  
Authenticated students shall be able to view: their current enrollment, subject-wise attendance %, fee invoice history and outstanding dues, library issued books, and published results — all from a single dashboard.

---

### 3.5 Attendance Management

**[ATT-001] Session Lifecycle:**  
An attendance session has three states: DRAFT (editable), SUBMITTED (locked for editing, pending HOD review if editing needed), LOCKED. Faculty create sessions in DRAFT and submit them. Only one session per offering+section+date is allowed.

**[ATT-002] Attendance Record:**  
Each attendance record ties a session to a student with a status: PRESENT, ABSENT, LATE, EXCUSED, or HOLIDAY. Re-marking is permitted while the session is in DRAFT — the service deletes existing records for the session and writes fresh ones (idempotent).

**[ATT-003] Post-Submission Edit:**  
Faculty may not edit attendance after submission without HOD approval. The system shall enforce this at the service layer.

**[ATT-004] Attendance Summary:**  
The system shall calculate a student's attendance percentage per subject offering as: (present count / total sessions count) × 100, rounded to one decimal place.

**[ATT-005] Eligibility Threshold:**  
Each course has a configurable minimum attendance percentage (default: 75%). Students below this threshold are ineligible for the end-term exam. The system shall flag ineligible students automatically.

**[ATT-006] Condonation Workflow:**  
Students may submit a condonation request when below threshold. The request includes a reason. HOD or Admin reviews and approves or rejects. Approval does not automatically change the attendance percentage — it is a flag used by the Exam Cell when generating hall tickets.

**[ATT-007] Parent Alerts:**  
When a student accumulates N consecutive absences (configurable per course policy, default: 3), the system shall queue a notification job for the linked parent accounts.

---

### 3.6 Examination and Results

**[EXAM-001] Exam Scheduling:**  
An exam is associated with a batch+term. Exam types include: INTERNAL_1, INTERNAL_2, MID_TERM, END_TERM, PRACTICAL, VIVA, PROJECT. An exam has start and end dates.

**[EXAM-002] Hall Ticket Eligibility:**  
Before generating hall tickets, the system shall check each student's attendance against the minimum threshold and mark ineligible students. Ineligibility reasons are recorded and visible to the Exam Cell.

**[EXAM-003] Marks Entry:**  
Faculty enter internal and external marks per student per subject offering. Marks are stored with status `DRAFT` initially, changing to `SUBMITTED` on faculty lock.

**[EXAM-004] Marks Moderation:**  
HOD or Exam Cell reviews submitted marks. On approval, status changes to `MODERATED`. On publication, status changes to `PUBLISHED`. Faculty cannot change marks once submitted without raising a correction request.

**[EXAM-005] Result Computation:**  
On publication, the system shall compute: total marks, pass/fail status, grade (per institution-configured grading scale), grade points, and SGPA for the term. CGPA is computed across all completed terms.

**[EXAM-006] Grade Card Generation:**  
The system shall generate a PDF grade card per student per term containing: student details, subject-wise marks, grades, SGPA, CGPA, and result status.

**[EXAM-007] Revaluation:**  
A student may apply for revaluation of a specific marks entry after result publication. The request records the reason. A revaluation fee invoice is generated. The Exam Cell assigns the paper to a re-evaluator. If marks change, an amended grade card is generated.

---

### 3.7 Finance and Accounting

**[FIN-001] Fee Structure:**  
Fee structures are defined per course + optional batch + fee head + term number. The same fee head can have different amounts per batch (e.g., 2024 intake pays more than 2022 intake). A null batch_id means the structure applies to all batches of that course.

**[FIN-002] Invoice Generation:**  
The system shall generate a fee invoice per student per term by summing all applicable fee structure entries. Duplicate invoices for the same student+batch+term are rejected.

**[FIN-003] Payment Collection:**  
A payment is recorded against an invoice with: amount, payment mode, transaction reference, collected-by user, and timestamp. The invoice's paid amount is updated. When fully paid, status changes to PAID; partially paid changes to PARTIAL.

**[FIN-004] Double-Entry Ledger:**  
Every fee payment shall generate two ledger entries: a DEBIT to the receivable account and a CREDIT to the fee income account. Manual journal entries by the Head Accountant shall also generate paired entries. The ledger table is append-only (trigger-enforced).

**[FIN-005] Late Fee:**  
If payment is received after the invoice due date, a late fee is calculated as: days_late × late_fee_per_day (from the applicable fee structure). Late fees are added to the invoice's late_fee field.

**[FIN-006] Scholarship and Waiver:**  
A scholarship or waiver reduces the net_amount on the invoice. Waivers above a configurable threshold require approval from the Head Accountant. All waivers are logged in the audit trail.

**[FIN-007] Refund:**  
A refund request is raised against an invoice. The request includes the amount, reason, and bank details. Approval routing is based on the amount threshold. On approval, a negative ledger entry is posted.

**[FIN-008] Accountant Permission Grants:**  
The Head Accountant may grant a sub-accountant access to: collect fees (optionally scoped to specific departments or fee heads), view ledger entries (optionally scoped to a department's cost center), process refunds up to a threshold, or process payroll. Grants are time-boxed and revocable.

**[FIN-009] Financial Year:**  
One financial year may be open at any time. Closing requires dual approval (Head Accountant + Admin). On close, no new ledger entries may be posted for the closed year. Re-opening requires the same dual approval with a reason.

**[FIN-010] Vendor and Procurement:**  
Vendors are maintained in a master list. Purchase orders reference a vendor, total amount, and financial year. A three-way match (PO → GRN → vendor invoice) is tracked before payment is released.

---

### 3.8 Library Management

**[LIB-001] Book Title Catalog:**  
Book titles are the bibliographic master: title, author, ISBN, edition, publisher, publication year, Dewey/LC classification, language. Total copies and available copies are maintained as derived counts on the title record.

**[LIB-002] Physical Copy Registration:**  
Each physical copy generates a unique accession number and barcode (Code 128). The barcode image is generated in-process using ZXing and stored via the file storage abstraction. One barcode = one physical copy. Multiple copies of the same title have independent barcodes.

**[LIB-003] Custody Log (Append-Only):**  
Every issue and return creates a row in `library_custody_log`. Rows record: copy ID, borrower details, issued-by staff, issue timestamp, due timestamp, return timestamp (nullable until returned), condition on issue, condition on return, and any fine amount. The table is append-only (trigger-enforced); corrections are new entries.

**[LIB-004] Issue Rules:**  
The system shall enforce before issuing: (a) copy is in AVAILABLE status; (b) borrower has no overdue books; (c) borrower has not reached their loan limit (per loan policy for their borrower type — STUDENT/FACULTY/STAFF).

**[LIB-005] Fine Calculation:**  
On return, if overdue, the fine is: days_late × fine_per_day (from loan policy), capped at max_fine if configured. The fine is recorded in both the custody log and a separate library_fines record.

**[LIB-006] Fine Waiver:**  
The Chief Librarian may waive a fine. Library Assistants may waive fines up to a configurable threshold if granted this permission by the Chief Librarian. All waivers are logged.

**[LIB-007] Reservation Queue:**  
When a title has no available copies, a student may join the reservation queue. On return, the system automatically notifies the next person in queue and holds the copy for a configurable pickup window (default: 48 hours).

**[LIB-008] Renewal Rules:**  
Renewals are permitted if: (a) the book is not overdue; (b) no active reservation exists on the title. Renewals extend the due date by the standard loan period for the borrower type.

**[LIB-009] Stock Verification:**  
The system shall support a stock-take mode where staff bulk-scan copy barcodes and the system identifies: found (matches expected location), missing (expected but not scanned), misplaced (scanned in wrong location). Results are saved as a stock-take record.

---

### 3.9 Asset Management

**[AST-001] Asset Registration:**  
Registering an asset generates a unique asset tag (auto-numbered, prefixed by category) and a barcode image. Asset records include: name, category, department, room location, purchase date, cost, vendor, warranty expiry.

**[AST-002] Asset Custody Log (Append-Only):**  
Every transfer of an asset between departments, rooms, or custodians creates a new row in `asset_custody_log`. The log records: from/to department, from/to user, transferred-by user, timestamp, reason, and locations. The table is append-only (trigger-enforced).

**[AST-003] Maintenance Tickets:**  
Any authenticated user may raise a maintenance ticket for an asset, specifying a title, description, and priority. Tickets are assigned to maintenance staff. Resolution records the fix and closes the ticket.

**[AST-004] Disposal:**  
Asset disposal requires: Asset Manager to initiate + Admin to confirm (if above value threshold). On disposal, the asset status is set to DISPOSED and an audit entry is written.

**[AST-005] Depreciation:**  
The system shall track a depreciation schedule per asset based on category's depreciation rate and purchase cost. Depreciation is calculated as a straight-line schedule and recorded annually.

---

### 3.10 HR and Leave Management

**[HR-001] Staff Profile:**  
Each staff member (teaching or non-teaching) has a profile linked to their user account. Profile includes: employee number (unique), department, designation, joining date, qualification, bank details for payroll, PAN/PF numbers.

**[HR-002] Leave Types and Balances:**  
Leave types (Casual, Sick, Earned, Maternity, Paternity, Loss of Pay) are configured with annual entitlements. Leave balances are maintained per user per leave type per year. Balances are initialized at the start of each academic year.

**[HR-003] Leave Application:**  
Staff apply for leave specifying type, date range (from/to), and reason. The system calculates days requested and checks against available balance before submission.

**[HR-004] Leave Approval:**  
Leave requests route through HOD → HR Manager approval chain. Each approver records their decision and remarks. On final approval, the used_days count is incremented on the leave balance.

---

### 3.11 Communication

**[COMM-001] Notices:**  
Notices are created by Admin or HOD and targeted to: ALL users, a specific department, a specific batch, or a specific role. Notices have a publish timestamp and optional expiry. Notices are displayed on the relevant users' dashboards.

**[COMM-002] Helpdesk Tickets:**  
Any authenticated user may raise a helpdesk ticket with a category (academic, financial, IT, facilities), title, and description. Tickets are assigned to the relevant department. Status progresses through OPEN → ASSIGNED → IN_PROGRESS → RESOLVED → CLOSED. Comments may be added by the ticket owner or the assigned staff.

---

### 3.12 Background Job Scheduler

**[SCHED-001] Job Queue:**  
Background jobs are persisted in the `background_jobs` table with type, JSON payload, status (PENDING/RUNNING/DONE/FAILED), attempt count, and scheduled timestamp.

**[SCHED-002] Job Polling:**  
A scheduled poller runs every 30 seconds. It selects due PENDING jobs using `SELECT ... FOR UPDATE SKIP LOCKED`, processes them, and marks them DONE or FAILED.

**[SCHED-003] Retry with Backoff:**  
Failed jobs are retried with exponential backoff: retry delay = 2^attempt_count × 30 seconds. Maximum attempts is configurable (default: 3). Jobs exceeding max attempts are marked FAILED and surfaced to the admin.

**[SCHED-004] Scheduled Jobs:**  
The following jobs run on a cron schedule:
- Attendance shortfall scan: nightly at 01:00
- Fee due-date reminder sweep: daily at 08:00
- Library overdue scan: nightly at 02:00
- Permission grant expiry: nightly at 03:00
- AMC renewal alerts: Mondays at 09:00

---

### 3.13 Audit Logging

**[AUDIT-001] Coverage:**  
The audit log shall capture: login/logout, login failures, all create/update/soft-delete operations on every entity, all financial transactions, all permission grant/revocation events, all result publication events, all custody log entries (library + asset).

**[AUDIT-002] Immutability:**  
The `audit_log` table shall have PostgreSQL triggers on UPDATE and DELETE that raise an exception, making the table physically append-only regardless of the connecting user.

**[AUDIT-003] Log Content:**  
Each audit entry contains: id, user_id, username, action (enum), module, entity_type, entity_id, description, before_value (JSON), after_value (JSON), ip_address, request_id, occurred_at.

**[AUDIT-004] Search:**  
Audit logs are searchable by: user, entity type, action, and date range. Results are paginated. The search endpoint is accessible only to SUPER_ADMIN, ADMIN, and AUDITOR roles.

---

## 4. External Interface Requirements

### 4.1 User Interfaces

**[UI-001]** The frontend shall be a React 18 single-page application (SPA) served as static assets by Nginx.  
**[UI-002]** The UI shall be responsive and function correctly on screens from 320px width (mobile) to 2560px (desktop).  
**[UI-003]** All environment-specific values (API base URL, feature flags) shall be injected via a runtime `env-config.js` written by the Nginx container entrypoint — not baked into the JavaScript bundle.  
**[UI-004]** All fonts, icons, and UI library assets shall be bundled at build time — no CDN reference at runtime.  
**[UI-005]** The UI shall display meaningful loading states and error messages. HTTP 4xx errors shall show the `detail` field from the RFC 7807 error response. HTTP 5xx errors shall show a generic message — never a stack trace.

### 4.2 API Interface

**[API-001]** All backend API endpoints shall follow the pattern: `POST/GET/PUT/PATCH/DELETE /api/v1/{module}/{resource}`.  
**[API-002]** All responses shall be wrapped in a consistent envelope: `{ success, message, data, timestamp }`.  
**[API-003]** Error responses shall follow RFC 7807: `{ type, title, status, detail, instance, requestId, timestamp, fieldErrors? }`.  
**[API-004]** All list endpoints shall be paginated. No endpoint may return an unbounded result set.  
**[API-005]** An OpenAPI 3 spec shall be auto-generated from controllers and available at `/v3/api-docs`. Swagger UI is available at `/swagger-ui.html` in non-production profiles only.  
**[API-006]** Financial transaction endpoints that create or post shall accept an optional `Idempotency-Key` header to allow safe client-side retry without duplicate processing.

### 4.3 Database Interface

**[DB-001]** The system connects to PostgreSQL via a HikariCP connection pool (default: min=5, max=20).  
**[DB-002]** All schema changes are managed by Flyway, numbered sequentially (V001, V002, …). No DDL may be executed outside Flyway migrations.  
**[DB-003]** JPA/Hibernate is used for all data access. No raw JDBC or string-concatenated SQL in application code.  
**[DB-004]** The `ddl-auto` property is set to `validate` — Hibernate validates against the schema but never modifies it.

### 4.4 Storage Interface

**[STORAGE-001]** All file operations go through the `FileStorageService` interface. No module accesses the underlying storage (filesystem, MinIO, S3) directly.  
**[STORAGE-002]** Files are identified by a UUID file ID, not by path, filename, or URL.  
**[STORAGE-003]** In air-gapped mode, the local filesystem implementation is automatically selected.

---

## 5. Non-Functional Requirements

### 5.1 Performance Requirements

**[NFR-PERF-001]** Standard read API (single entity, paginated list): p95 response time < 300 ms under normal load.  
**[NFR-PERF-002]** Write API (create, update): p95 response time < 500 ms.  
**[NFR-PERF-003]** Heavy report generation: submitted as background job; result available within 2 minutes.  
**[NFR-PERF-004]** The system shall handle 300 concurrent users on a 4 vCPU / 8 GB RAM server without response time degradation beyond NFR-PERF-001.  
**[NFR-PERF-005]** No API endpoint may perform an unbounded database query. All list queries must have a LIMIT clause, enforced via Spring Data pagination.

### 5.2 Security Requirements

**[NFR-SEC-001]** All inter-service communication over HTTPS. No plain HTTP in production.  
**[NFR-SEC-002]** Passwords hashed with BCrypt, cost factor ≥ 12.  
**[NFR-SEC-003]** JWT signed with HS256. Secret must be ≥ 256 bits. Secret must not appear in logs or error responses.  
**[NFR-SEC-004]** Refresh tokens stored as SHA-256 hash. Raw tokens never logged or stored.  
**[NFR-SEC-005]** No sensitive data (passwords, tokens, Aadhar numbers, bank account numbers) appears in application logs.  
**[NFR-SEC-006]** All SQL parameters passed via JPA bind parameters — no string concatenation.  
**[NFR-SEC-007]** CORS: allowed origins are explicitly configured. Wildcard (`*`) origins are not permitted in production.  
**[NFR-SEC-008]** HTTP response headers include: `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Content-Security-Policy`.

### 5.3 Reliability Requirements

**[NFR-REL-001]** System uptime target: 99.5% (excluding planned maintenance windows).  
**[NFR-REL-002]** No committed database transaction shall be lost on application crash (PostgreSQL WAL durability guarantee).  
**[NFR-REL-003]** Background job failures shall not cause data loss; jobs are retried and permanently logged on final failure.  
**[NFR-REL-004]** The system shall include a daily backup verification job. Admin is alerted if the most recent backup is older than 25 hours or fails integrity check.

### 5.4 Maintainability Requirements

**[NFR-MAINT-001]** No business logic in controller classes. Controllers call exactly one service method per endpoint.  
**[NFR-MAINT-002]** No module imports another module's JPA repository. Cross-module data access only via published service interfaces.  
**[NFR-MAINT-003]** Common utilities (date formatting, pagination helpers, slug generation, validation annotations) are in the `common` package and imported — never copied.  
**[NFR-MAINT-004]** Frontend shared components (DataTable, FormBuilder, BarcodeScanner, PermissionGuard) are in `src/shared/components` and imported — never duplicated per module.  
**[NFR-MAINT-005]** All dependency versions are pinned in `pom.xml` and `package.json`. No floating versions.

### 5.5 Scalability Requirements

**[NFR-SCALE-001]** The application tier is stateless (JWT-based, no HttpSession). It can be horizontally scaled by adding instances behind a load balancer without session stickiness.  
**[NFR-SCALE-002]** The background job queue uses `SELECT ... FOR UPDATE SKIP LOCKED` — safe for multiple concurrent application instances polling the same job table.  
**[NFR-SCALE-003]** Cache is Caffeine (in-process) by default. Redis can be swapped in for multi-instance deployments by changing the cache configuration property.

### 5.6 Portability Requirements

**[NFR-PORT-001]** The same compiled JAR and frontend build assets shall run identically in cloud, on-premises, and air-gapped modes. The only variation is the Spring profile and environment variables.  
**[NFR-PORT-002]** No platform-specific file paths, OS commands, or proprietary cloud SDK calls in business logic.  
**[NFR-PORT-003]** Deployment is via Docker Compose. The compose file is the single source of truth for how services connect.

---

## 6. Data Requirements

### 6.1 Data Retention

| Data Category | Retention Policy |
|---|---|
| Student academic records | Permanent (never deleted) |
| Financial transactions and ledger entries | Permanent (append-only) |
| Library custody log | Permanent (append-only) |
| Asset custody log | Permanent (append-only) |
| Audit log | Minimum 7 years |
| Refresh tokens | 7 days after expiry, then auto-deleted |
| Background jobs (DONE) | 7 days after completion, then auto-deleted |
| Session / login records | 90 days |

### 6.2 Data Integrity Rules

1. Student enrollment numbers are immutable after creation
2. Ledger entries are immutable after posting
3. Library custody log rows are immutable once the return timestamp is set
4. Asset custody log rows are never modified
5. Audit log rows are never modified or deleted
6. A financial year in CLOSED status cannot receive new ledger entries
7. Marks in PUBLISHED status cannot be edited without a revaluation workflow

### 6.3 Data Privacy

1. Aadhar numbers are stored but never returned in API responses unless explicitly requested by an Admin with audit logging
2. Bank account numbers and IFSC codes are write-only from the UI — they are stored but masked on retrieval
3. TOTP secrets are stored encrypted at rest
4. Student date-of-birth is not exposed on list endpoints — only on full profile retrieval

---

## 7. System Constraints

1. **Single database engine:** PostgreSQL 15+ only. No Oracle, MySQL, SQL Server, or SQLite support.
2. **Java 21 LTS:** The backend requires Java 21. No other JVM version is supported.
3. **No message broker:** RabbitMQ, Kafka, and similar are explicitly excluded. The DB-backed job queue is the only async mechanism.
4. **No server-side rendering:** The frontend is a static SPA. No Next.js, Nuxt, or SSR framework is used.
5. **No CDN at runtime:** All frontend assets must be self-contained in the build output.
6. **Docker required for on-prem:** Bare-metal installation without Docker is not a supported deployment mode.

---

## 8. Assumptions and Dependencies

### 8.1 Assumptions

1. The target institution has at least one person capable of running `docker compose up -d`
2. The institution's server has at least 8 GB RAM and 4 CPU cores for a small college
3. Browser clients support ES2020+ JavaScript (no IE11 support)
4. The institution accepts PostgreSQL as their database (no Oracle/SQL Server mandate)
5. For air-gapped deployments, a mechanism exists to transfer files into the network (e.g., approved USB policy)

### 8.2 External Dependencies

| Dependency | Version | Purpose | Air-Gap Safe |
|---|---|---|---|
| Spring Boot | 3.3.x | Application framework | Yes |
| Spring Security | 6.x (via Boot) | Auth and access control | Yes |
| PostgreSQL | 15+ | Primary database | Yes |
| Flyway | 10.x (via Boot) | Schema migrations | Yes |
| JJWT | 0.12.x | JWT creation/validation | Yes |
| ZXing | 3.5.x | Barcode/QR generation | Yes |
| MapStruct | 1.5.x | DTO mapping | Yes |
| Lombok | 1.18.x | Boilerplate reduction | Yes |
| React | 18.x | Frontend framework | Yes |
| TanStack Query | 5.x | Server state management | Yes |
| Zustand | 4.x | UI state management | Yes |
| MUI | 5.x | React UI component library | Yes (bundled) |
| Vite | 5.x | Frontend build tool | Yes |
| MinIO SDK | 8.x | Optional object storage | Yes |
| AWS SDK | 2.x | Optional S3 storage | No (cloud only) |

---

## 9. Appendix — Glossary

| Term | Definition |
|---|---|
| Accession Number | Unique sequential identifier for a physical library book copy (e.g., 2024-000001) |
| Asset Tag | Auto-generated unique identifier for a physical college asset (e.g., CMP-000001) |
| Audit Trail | Immutable log of who did what, when, and to what data |
| Chain of Custody | The sequential record of every person who has held a specific library copy or asset |
| Condonation | Formal exemption granted to a student who is below the minimum attendance threshold |
| Double-Entry Bookkeeping | Every financial transaction is recorded as both a DEBIT and a CREDIT entry |
| Enrollment | The formal record binding a student to a batch, section, and term |
| Financial Year | The accounting period (typically April to March in India) during which books are kept |
| Idempotency Key | A client-supplied unique key on a write request that prevents duplicate processing on retry |
| Loan Policy | Rules per borrower type defining: max books, loan days, fine per day, max fine |
| Modular Monolith | A single deployable application internally organized into independent feature modules |
| SGPA | Semester Grade Point Average — weighted average of grade points for one semester |
| CGPA | Cumulative Grade Point Average — weighted average across all completed semesters |
| Skip Locked | A PostgreSQL locking hint that skips rows already locked by another transaction |
| Subject Offering | A specific subject taught to a specific batch in a specific term |
| Tenant | In cloud (multi-tenant) mode, each institution is a separate tenant with row-level data isolation |

---

*Document Owner: Engineering Team*  
*Last Updated: June 2026*  
*Next Review: After Phase 0 implementation*
