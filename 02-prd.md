# College ERP System — Product Requirements Document (PRD)

**Version:** 1.0  
**Date:** June 2026  
**Status:** Approved  
**Owner:** Product Team

---

## Table of Contents

1. [Product Overview](#1-product-overview)
2. [User Personas](#2-user-personas)
3. [User Stories by Module](#3-user-stories-by-module)
4. [Feature Requirements](#4-feature-requirements)
5. [Non-Functional Requirements](#5-non-functional-requirements)
6. [Permission Matrix Summary](#6-permission-matrix-summary)
7. [Notification Requirements](#7-notification-requirements)
8. [Integration Requirements](#8-integration-requirements)
9. [Reporting Requirements](#9-reporting-requirements)
10. [Acceptance Criteria](#10-acceptance-criteria)

---

## 1. Product Overview

The College ERP is a web-based platform that manages the full lifecycle of a higher education institution — from student admission through graduation. It runs as a Java Spring Boot backend serving a React JavaScript frontend, backed by PostgreSQL.

### 1.1 Core Value Propositions

- **One login, everything** — a student sees attendance, fees, library, notices, and results from one portal
- **Immutable audit trail** — every financial entry, grade change, and asset transfer is permanently logged
- **Scoped financial access** — the Head Accountant controls exactly what each sub-accountant can see and do
- **Per-copy library tracking** — every physical book copy has its own barcode and chain-of-custody history
- **Works offline** — designed to operate on a fully air-gapped network

---

## 2. User Personas

### Persona 1 — Arjun (Student, 19 years old)

> "I want to check my attendance from my phone before the semester ends so I know if I'm eligible for exams."

**Goals:** Check attendance %, view fee dues, download hall tickets, check results  
**Pain points:** Has to visit the office for every certificate, doesn't know fine amounts until collection day  
**Tech comfort:** High — uses smartphone daily

---

### Persona 2 — Mrs. Lakshmi (Faculty, 38 years old)

> "I spend 20 minutes every day copying attendance from paper to Excel. There has to be a better way."

**Goals:** Quick digital attendance marking, grade submission, leave application  
**Pain points:** Paper-based systems, no central place to upload study material  
**Tech comfort:** Medium — comfortable with web forms

---

### Persona 3 — Mr. Ramesh (Head Accountant, 52 years old)

> "I need to know exactly who collected what fees, when, and I need to be able to limit what my junior staff can touch."

**Goals:** Full financial control, scoped access for juniors, clean audit trail  
**Pain points:** Junior accountants with too much access, no reconciliation tool  
**Tech comfort:** Medium — uses Tally currently

---

### Persona 4 — Dr. Meena (Principal / Admin, 55 years old)

> "At the end of each month I want a report — how much was collected, who defaulted, what the attendance rate was — without calling three different people."

**Goals:** Dashboard visibility, one-click reports, compliance evidence  
**Pain points:** Data scattered across systems, report preparation takes days  
**Tech comfort:** Low-medium — prefers dashboards over raw data

---

### Persona 5 — Suresh (Chief Librarian, 44 years old)

> "When a book goes missing, I need to know exactly who had it last, and the one before that."

**Goals:** Per-copy tracking, barcode labels, fine management  
**Pain points:** No history of who borrowed what copy; fines are verbal only  
**Tech comfort:** Medium

---

### Persona 6 — IT Admin Karthik (28 years old)

> "I need to install this on our campus server — we have no internet access. It must just work."

**Goals:** Simple deployment, minimal dependencies, clear error messages  
**Pain points:** Complex software that needs cloud licensing or internet to function  
**Tech comfort:** High

---

## 3. User Stories by Module

### 3.1 Authentication

| ID | As a... | I want to... | So that... | Priority |
|---|---|---|---|---|
| AUTH-1 | Any user | Log in with username or email + password | I can access the system | Must |
| AUTH-2 | Any user | Receive a new access token using my refresh token | I stay logged in without re-entering my password | Must |
| AUTH-3 | Admin | Force-logout any user's all sessions | I can respond to a security incident | Must |
| AUTH-4 | Any user | Change my own password | I can maintain security | Must |
| AUTH-5 | Admin | Enable 2FA requirement for sensitive roles | Accountants and admins have extra security | Should |
| AUTH-6 | Any user | Get a clear error message after failed login | I know whether I typed the wrong password or am locked out | Must |

### 3.2 Academic Management

| ID | As a... | I want to... | So that... | Priority |
|---|---|---|---|---|
| ACAD-1 | Admin | Create departments with codes and HOD assignments | The system reflects the college's structure | Must |
| ACAD-2 | Admin | Create courses under departments with level, duration, intake | Each program is fully defined | Must |
| ACAD-3 | Admin | Create batches per course per academic year | Each intake year is tracked separately | Must |
| ACAD-4 | Admin/HOD | Add sections (A, B, C) to a batch | Classes can be split into divisions | Must |
| ACAD-5 | Admin | Define subjects with credits and type (theory/lab/elective) | The curriculum is fully catalogued | Must |
| ACAD-6 | Admin/HOD | Map subjects to batches+terms (subject offerings) | The timetable knows which subject is taught when | Must |
| ACAD-7 | HOD | Assign faculty to subject offerings per section | Each class has an assigned teacher | Must |
| ACAD-8 | Admin | Promote a batch to the next term | The batch's semester counter advances | Must |

### 3.3 Student Management

| ID | As a... | I want to... | So that... | Priority |
|---|---|---|---|---|
| STU-1 | Admin | Enroll a new student and assign to a batch+section | The student is in the system | Must |
| STU-2 | Admin/HOD | Transfer a student to a different section | Section changes are handled gracefully | Must |
| STU-3 | Admin | View a student's full enrollment history | Every batch/section they've been in is recorded | Must |
| STU-4 | Student | View my own profile, attendance, and fee dues from my dashboard | I have self-service access | Must |
| STU-5 | Admin | Mark a student as graduated | The student's record is closed correctly | Must |
| STU-6 | Parent | View my linked child's attendance and fee status | I'm informed without calling the college | Must |
| STU-7 | Student | Apply for a bonafide/transfer certificate online | I don't need to visit the office | Should |

### 3.4 Attendance

| ID | As a... | I want to... | So that... | Priority |
|---|---|---|---|---|
| ATT-1 | Faculty | Open an attendance session for my class today | I can mark who is present/absent | Must |
| ATT-2 | Faculty | Mark each student as Present/Absent/Late for a session | Attendance is recorded digitally | Must |
| ATT-3 | Faculty | Submit (lock) a session once marking is done | The session can't be accidentally changed | Must |
| ATT-4 | HOD | Review and approve edits to submitted attendance | Corrections go through approval | Must |
| ATT-5 | Student | View my attendance % per subject | I can monitor my eligibility | Must |
| ATT-6 | Student | Apply for attendance condonation if I'm below threshold | I can formally request an exception | Should |
| ATT-7 | Parent | Receive an automatic notification when my child misses 3 consecutive classes | I'm alerted to potential issues | Must |
| ATT-8 | Admin/HOD | Download a defaulter list for a batch | I can take action on students below threshold | Must |

### 3.5 Examination & Results

| ID | As a... | I want to... | So that... | Priority |
|---|---|---|---|---|
| EXAM-1 | Exam Cell | Schedule exams for a batch+term | The timetable is fixed | Must |
| EXAM-2 | Exam Cell | Generate and issue hall tickets for eligible students | Only eligible students get admission | Must |
| EXAM-3 | Faculty | Enter internal and external marks for my subject | Marks are recorded digitally | Must |
| EXAM-4 | HOD | Moderate and lock submitted marks before publication | A second pair of eyes checks the marks | Must |
| EXAM-5 | Admin/Exam Cell | Publish results after HOD moderation | Students can see their grades | Must |
| EXAM-6 | Student | Download my grade card as a PDF | I have an official document | Must |
| EXAM-7 | Student | Apply for revaluation of a specific paper | I can dispute a grade formally | Should |

### 3.6 Finance

| ID | As a... | I want to... | So that... | Priority |
|---|---|---|---|---|
| FIN-1 | Head Accountant | Define fee structures per course/batch/term | Invoices can be auto-generated | Must |
| FIN-2 | Head Accountant | Generate fee invoices for all students in a batch | Every student has a fee record | Must |
| FIN-3 | Accountant | Record a fee payment against an invoice | The receipt is issued and ledger is updated | Must |
| FIN-4 | Student/Parent | Pay fees online and download the receipt | Payment is convenient and documented | Must |
| FIN-5 | Head Accountant | Grant a sub-accountant access only to specific fee heads or departments | Least-privilege access is enforced | Must |
| FIN-6 | Head Accountant | View the full institutional ledger and generate financial statements | Financial reporting is available | Must |
| FIN-7 | Accountant | View a defaulter list and send reminders | Outstanding fees are actively managed | Must |
| FIN-8 | Admin+Head Accountant | Close the financial year (requires both approvals) | Books are locked at year-end | Must |
| FIN-9 | Head Accountant | Apply a scholarship or fee waiver to an invoice | Concessions are formally recorded | Must |
| FIN-10 | Auditor | View financial records and audit trail in read-only mode | External audits are supported | Must |

### 3.7 Library

| ID | As a... | I want to... | So that... | Priority |
|---|---|---|---|---|
| LIB-1 | Chief Librarian | Add a book title to the catalog | The title exists in the system | Must |
| LIB-2 | Chief Librarian/Asst | Add a physical copy to a title (auto-generates barcode) | Each copy is uniquely trackable | Must |
| LIB-3 | Chief Librarian/Asst | Print the barcode label for a copy | The physical label is on the book | Must |
| LIB-4 | Library Assistant | Issue a book by scanning the barcode + student ID | The issue is recorded in the custody chain | Must |
| LIB-5 | Library Assistant | Process a return by scanning the barcode | The return is logged and fines calculated | Must |
| LIB-6 | Library Assistant | Renew a book if no reservation exists | The due date is extended | Must |
| LIB-7 | Chief Librarian | View the full custody chain for any copy | I can see every hand the book passed through | Must |
| LIB-8 | Student | Reserve a title that is currently issued | I'm notified when it becomes available | Should |
| LIB-9 | Chief Librarian | Waive a fine with a reason | Exceptions are documented | Should |
| LIB-10 | Chief Librarian | Mark a copy as lost and bill the borrower | Lost items are handled formally | Must |

### 3.8 Asset Management

| ID | As a... | I want to... | So that... | Priority |
|---|---|---|---|---|
| AST-1 | Asset Manager | Register an asset with auto-generated barcode tag | Every asset is in the registry | Must |
| AST-2 | Asset Manager | Transfer an asset to another department/room | Moves are tracked in the custody chain | Must |
| AST-3 | Asset Manager | View the full custody chain for any asset | I can see where it has been | Must |
| AST-4 | Any staff | Raise a maintenance ticket for a broken asset | Faults are formally reported | Must |
| AST-5 | Asset Manager/Maintenance | Resolve and close a maintenance ticket | Repairs are documented | Must |
| AST-6 | Asset Manager+Admin | Dispose of an asset with a reason | Write-offs are logged | Must |

### 3.9 HR & Leave

| ID | As a... | I want to... | So that... | Priority |
|---|---|---|---|---|
| HR-1 | HR Manager | Create a staff profile for a new employee | They are in the HR system | Must |
| HR-2 | Any staff | Apply for leave with dates and reason | The request is formally submitted | Must |
| HR-3 | HOD/HR | Approve or reject a leave request | The decision is recorded | Must |
| HR-4 | Any staff | View my leave balance for the current year | I know how many days I have left | Must |

---

## 4. Feature Requirements

### 4.1 Authentication & Security

**FR-AUTH-1:** The system SHALL support username/password authentication with bcrypt (cost factor ≥ 12).  
**FR-AUTH-2:** The system SHALL issue a short-lived JWT access token (15 min) and a long-lived refresh token (7 days, stored as SHA-256 hash).  
**FR-AUTH-3:** The system SHALL lock an account after N consecutive failed attempts (configurable, default 5) for a configurable duration (default 30 min).  
**FR-AUTH-4:** The system SHALL support TOTP-based 2FA without SMS dependency.  
**FR-AUTH-5:** The system SHALL maintain and expose an active session list per user, with force-logout capability for admins.  
**FR-AUTH-6:** Refresh tokens SHALL be stored as SHA-256 hashes — never the raw token.

### 4.2 Role-Based Access Control

**FR-RBAC-1:** The system SHALL implement granular, scope-based permissions. A permission grant SHALL carry: permission key, optional department scope, optional batch scope, optional fee-head scope, optional amount threshold, effective from/until dates.  
**FR-RBAC-2:** Permission grants SHALL be created only by authorized superior roles (Head Accountant → Accountant, Chief Librarian → Library Assistant, Admin → HOD/Faculty, etc.)  
**FR-RBAC-3:** A user SHALL NOT be able to grant themselves permissions.  
**FR-RBAC-4:** All permission grant and revocation events SHALL be recorded in the audit log.  
**FR-RBAC-5:** A single user creating AND approving the same financial transaction above the configured threshold SHALL be prevented by the system.

### 4.3 Barcode Generation

**FR-BARCODE-1:** The system SHALL generate Code 128 barcodes for every library book copy and asset using ZXing (pure Java, no external service).  
**FR-BARCODE-2:** Generated barcodes SHALL be stored as PNG files via the file storage abstraction.  
**FR-BARCODE-3:** Barcodes SHALL be downloadable as printable label sheets (PDF format).

### 4.4 Audit Trail

**FR-AUDIT-1:** Every write operation (create, update, soft-delete) in every module SHALL produce an audit log entry with: user ID, username, action type, entity type, entity ID, description, before/after JSON snapshot, request ID, and timestamp.  
**FR-AUDIT-2:** The audit log table SHALL be append-only, enforced by a PostgreSQL trigger that rejects UPDATE and DELETE statements.  
**FR-AUDIT-3:** The audit log SHALL be searchable by user, entity type, action, and date range.

### 4.5 File Storage

**FR-STORAGE-1:** The system SHALL support three storage backends: local filesystem, MinIO, and AWS S3.  
**FR-STORAGE-2:** The active backend SHALL be selected by a single configuration property (`erp.storage.mode`).  
**FR-STORAGE-3:** In air-gapped mode, the default SHALL be local filesystem with no external call.  
**FR-STORAGE-4:** File deletion SHALL be a soft operation — files are marked deleted, not physically removed, for audit compliance.

### 4.6 Notifications

**FR-NOTIF-1:** In-app notifications SHALL always be delivered regardless of deployment mode.  
**FR-NOTIF-2:** Email and SMS channels SHALL be optional and configurable via properties.  
**FR-NOTIF-3:** A notification channel failure SHALL never cause the originating business transaction to fail.  
**FR-NOTIF-4:** Notifications SHALL be persisted in the database so users see them on next login even if they were offline when the notification fired.  
**FR-NOTIF-5:** Real-time delivery SHALL use Server-Sent Events (SSE) with a polling fallback.

---

## 5. Non-Functional Requirements

### 5.1 Performance

| Requirement | Target |
|---|---|
| API response time (p95) for standard queries | < 300 ms |
| API response time (p95) for paginated list endpoints | < 500 ms |
| Report generation (lightweight) | < 5 seconds |
| Report generation (heavy, async) | Submitted as background job; result available within 2 minutes |
| Page load time (React SPA initial) | < 3 seconds on 4G connection |
| Maximum concurrent users (small college, single server) | 300 |
| Maximum concurrent users (large college, standard hardware) | 800 |

### 5.2 Security

| Requirement | Detail |
|---|---|
| Password hashing | BCrypt, cost factor 12 |
| Transport security | HTTPS enforced in all deployment modes |
| JWT algorithm | HS256 with secret ≥ 256 bits |
| Sensitive data logging | Passwords, tokens, Aadhar numbers NEVER appear in logs |
| SQL injection | Prevented by JPA/Hibernate parameterized queries — no raw string SQL in services |
| XSS | JSON API; Content-Security-Policy header on all responses |
| CORS | Configured explicitly; wildcard origins not allowed in production |

### 5.3 Reliability

- The system SHALL NOT lose any committed financial transaction, even on application crash (PostgreSQL durability)
- Background job failures SHALL be retried with exponential backoff up to a configurable limit
- Failed jobs SHALL be visible to admins, not silently dropped
- The system SHALL include a daily backup verification job that alerts admin on failure

### 5.4 Maintainability

- No business logic in controllers — controllers call services only
- No direct database queries across module boundaries — modules communicate via service interfaces
- Every shared utility (date formatting, pagination, validation) is written once in `common` and imported
- Flyway migrations are forward-only — no repeatable scripts for schema changes

### 5.5 Scalability

- The application tier is stateless (JWT auth, no server-side session) and can be horizontally scaled
- In-process Caffeine cache (zero extra infrastructure) scales to single-instance; Redis is opt-in for multi-instance
- Database pagination is mandatory on all list endpoints — no unbounded queries

### 5.6 Accessibility

- The React frontend SHALL meet WCAG 2.1 Level AA
- All interactive elements SHALL be keyboard-navigable
- Color is not used as the sole indicator of state

---

## 6. Permission Matrix Summary

> Full permission detail is in the Roles & Permissions document. This section captures the key business rules.

### Key Permission Rules

1. **Head Accountant** is the only role that can create/scope other accountant accounts.
2. **Chief Librarian** is the only role that can create/scope Library Assistant accounts.
3. **Financial year close** requires BOTH Head Accountant AND Admin to approve.
4. **Result publication** requires HOD or Exam Cell to lock marks — faculty alone cannot publish.
5. **Attendance edit** after submission requires HOD approval.
6. **Asset disposal** requires Asset Manager plus Admin sign-off above a value threshold.
7. **Custody logs** (library + asset) are readable by authorized roles but writable by no one — all writes are system-generated.
8. **Students** can see only their own records; cross-student data access is never permitted.

---

## 7. Notification Requirements

### Automatic Triggers

| Event | Recipient | Channel |
|---|---|---|
| Student misses 3 consecutive classes | Parent + Student | In-App + SMS (if enabled) |
| Student attendance drops below minimum threshold | Student + Parent | In-App + Email |
| Fee invoice generated | Student + Parent | In-App + Email |
| Fee due date approaching (3 days) | Student + Parent | In-App + SMS |
| Fee payment received | Student | In-App + Email (receipt) |
| Exam result published | Student | In-App |
| Library book due tomorrow | Student | In-App + SMS |
| Library book overdue | Student | In-App + SMS (daily) |
| Reserved book available for pickup | Student | In-App + SMS |
| Leave request approved/rejected | Staff member | In-App + Email |
| Maintenance ticket assigned | Maintenance staff | In-App |
| Maintenance ticket resolved | Reporter | In-App |
| AMC renewal due within 30 days | Asset Manager | In-App |

---

## 8. Integration Requirements

### 8.1 Required (built-in)

- **PostgreSQL** — primary database, via JDBC/JPA
- **SMTP** — email notifications (configurable, optional in air-gap)
- **ZXing** — barcode/QR generation (pure Java, no network)
- **Flyway** — database migrations (runs on startup, no network needed)

### 8.2 Optional (configured via properties)

- **MinIO** — on-premises object storage
- **AWS S3** — cloud object storage
- **SMS Gateway** — Twilio / MSG91 / AWS SNS (disabled in air-gap profile)
- **SAML/OIDC IdP** — optional SSO (local login always available as fallback)

### 8.3 Not Required (explicit exclusions)

- No external license server call
- No crash reporting / telemetry call
- No CDN for frontend assets (all bundled at build time)
- No map tile server in air-gap profile
- No npm/Maven registry access at runtime

---

## 9. Reporting Requirements

### 9.1 Standard Reports (auto-generated, no custom builder needed)

| Report | Available to | Format |
|---|---|---|
| Student fee defaulter list | Admin, Head Accountant, Accountant | PDF, Excel |
| Batch attendance summary | Admin, HOD, Faculty | PDF, Excel |
| Subject-wise attendance defaulter list | Admin, HOD, Exam Cell | PDF |
| Term results / grade cards | Student, Admin, Exam Cell | PDF |
| Financial statement (Income & Expenditure) | Head Accountant, Auditor | PDF, Excel |
| Department-wise budget vs. actual | Admin, HOD, Head Accountant | Excel |
| Library circulation report | Chief Librarian | Excel |
| Asset inventory by department | Admin, Asset Manager | Excel |
| Leave utilization report | HR Manager | Excel |

### 9.2 Custom Report Builder

- Available to Admin and Head Accountant
- Drag-and-drop column selector across available data entities
- Filter by date range, department, batch, term
- Heavy reports run asynchronously as background jobs; user is notified when ready
- Export formats: PDF, Excel (XLSX), CSV

---

## 10. Acceptance Criteria

### AC-1: Fee Collection End-to-End
**Given** a student has an unpaid fee invoice  
**When** the accountant records a payment via the Fee Collection screen  
**Then** a receipt is generated, the invoice status updates to PARTIAL or PAID, a double-entry ledger pair is posted, and an in-app notification is sent to the student — all within the same database transaction.

### AC-2: Library Custody Chain
**Given** a book copy with barcode LIB-2024-000001 has been issued to three different students over its lifetime  
**When** a librarian queries the custody chain for that copy  
**Then** all three issue-and-return records are returned in chronological order with borrower name, issue date, return date, and condition at return.

### AC-3: Attendance Shortfall Notification
**Given** a student's attendance in a subject drops to 70% (below the 75% minimum)  
**When** the nightly attendance shortfall scan job runs  
**Then** an in-app notification is sent to the student and an alert is sent to the linked parent account.

### AC-4: Air-Gap Zero Outbound
**Given** the application is running in `airgap` Spring profile  
**When** any user action is performed (login, attendance, fee payment, library issue)  
**Then** no HTTP call leaves the local Docker network (verified by network interception in E2E tests).

### AC-5: Accountant Scope Enforcement
**Given** Accountant A has been granted fee collection access scoped to the Computer Science department only  
**When** Accountant A attempts to collect a fee for a Mechanical Engineering student  
**Then** the API returns HTTP 403 Forbidden.

### AC-6: Financial Year Dual Approval
**Given** the Head Accountant has submitted a year-close request  
**When** only the Head Accountant has approved (Admin has not)  
**Then** the financial year remains OPEN and books are not locked.

---

*Document Owner: Product Team*  
*Last Updated: June 2026*  
*Next Review: Sprint Planning — Phase 0*
