# College ERP System — Database Schema Document

**Version:** 1.0  
**Date:** June 2026  
**Database Engine:** PostgreSQL 15+  
**Migration Tool:** Flyway (forward-only, numbered V001–V014)

---

## Table of Contents

1. [Schema Design Principles](#1-schema-design-principles)
2. [Entity Relationship Overview](#2-entity-relationship-overview)
3. [Naming Conventions](#3-naming-conventions)
4. [Base Column Pattern](#4-base-column-pattern)
5. [Module: Auth & Security](#5-module-auth--security)
6. [Module: Academic](#6-module-academic)
7. [Module: Student](#7-module-student)
8. [Module: Attendance](#8-module-attendance)
9. [Module: Examination](#9-module-examination)
10. [Module: Finance](#10-module-finance)
11. [Module: Library](#11-module-library)
12. [Module: Asset](#12-module-asset)
13. [Module: HR](#13-module-hr)
14. [Module: Hostel & Transport](#14-module-hostel--transport)
15. [Module: Communication](#15-module-communication)
16. [Module: Notifications & Scheduler](#16-module-notifications--scheduler)
17. [Append-Only Tables and Triggers](#17-append-only-tables-and-triggers)
18. [Indexes Summary](#18-indexes-summary)
19. [Seed Data (V014)](#19-seed-data-v014)

---

## 1. Schema Design Principles

### 1.1 Primary Keys
All tables use `UUID` primary keys generated via PostgreSQL's `gen_random_uuid()`. UUIDs are preferred over serial integers because:
- They are safe to generate on the application side before insert
- They do not leak sequential information to clients
- They are safe across distributed systems if ever needed

### 1.2 Soft Delete
All master-data tables include `is_deleted BOOLEAN NOT NULL DEFAULT FALSE`. Records are never physically deleted — they are marked `is_deleted = true`. All queries filter `WHERE is_deleted = FALSE` by default.

### 1.3 Append-Only Tables
The following tables are **physically append-only**, enforced by PostgreSQL triggers that RAISE EXCEPTION on any UPDATE or DELETE attempt:
- `audit_log`
- `ledger_entries`
- `library_custody_log`
- `asset_custody_log`

The `library_custody_log` has a **partial immutability trigger** — the core issue fields (copy_id, borrower_user_id, issued_at) are immutable, but return-related fields (returned_at, returned_by_user_id, condition_on_return, fine_amount) can be updated once.

### 1.4 Timestamps
All tables use `TIMESTAMPTZ` (timestamp with timezone) stored in UTC. Date-only fields use `DATE`.

### 1.5 Monetary Values
All monetary amounts use `NUMERIC(14,2)` — no FLOAT or DOUBLE. This avoids floating-point rounding errors in financial calculations.

---

## 2. Entity Relationship Overview

```
users
  ├── role_assignments          (user → role, department scope)
  ├── permission_grants         (user → named permission + scope)
  └── refresh_tokens

departments
  └── courses
        └── batches
              ├── sections
              │     ├── enrollments ──── students
              │     │                        └── guardians
              │     └── attendance_sessions
              │               └── attendance_records
              └── subject_offerings ─── subjects ─── departments
                        ├── faculty_subject_mappings ─── users
                        ├── marks_entries ─── students
                        └── exams
                              └── hall_tickets ─── students

financial_years
  ├── account_heads
  ├── ledger_entries (append-only)
  ├── fee_structures ─── fee_heads ─── courses
  ├── fee_invoices ─── students
  │     └── fee_payments
  ├── purchase_orders ─── vendors
  └── accountant_permission_grants ─── users

book_titles
  └── book_copies
        └── library_custody_log (append-only)
              └── library_fines

assets ─── asset_categories
  └── asset_custody_log (append-only)
  └── maintenance_tickets

audit_log (append-only)
background_jobs
notification_records
```

---

## 3. Naming Conventions

| Convention | Rule | Example |
|---|---|---|
| Table names | `snake_case`, plural | `fee_invoices`, `book_copies` |
| Column names | `snake_case` | `invoice_number`, `is_deleted` |
| Primary key | Always `id UUID` | `id UUID PRIMARY KEY` |
| Foreign keys | `{referenced_table_singular}_id` | `student_id`, `course_id` |
| Boolean columns | `is_` prefix | `is_deleted`, `is_active`, `is_current` |
| Timestamp columns | `_at` suffix | `created_at`, `paid_at`, `revoked_at` |
| Date columns | `_date` suffix | `due_date`, `joining_date`, `purchase_date` |
| Index names | `idx_{table}_{column(s)}` | `idx_students_enrollment_number` |
| Unique constraint names | `uq_{table}_{columns}` | `uq_sections_batch_name` |
| Trigger names | `trg_{table}_{action}` | `trg_audit_log_no_update` |

---

## 4. Base Column Pattern

All soft-deletable master-data tables include these base columns:

```sql
id          UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
is_deleted  BOOLEAN      NOT NULL DEFAULT FALSE,
created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
created_by  UUID,        -- FK to users.id (nullable for system-generated rows)
updated_by  UUID         -- FK to users.id
```

Append-only audit/log tables omit `is_deleted`, `updated_at`, and `updated_by` since they are never modified.

---

## 5. Module: Auth & Security

### 5.1 `users`

```sql
CREATE TABLE users (
    id                    UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    username              VARCHAR(50)  NOT NULL UNIQUE,
    email                 VARCHAR(150) NOT NULL UNIQUE,
    password_hash         TEXT         NOT NULL,
    full_name             VARCHAR(150) NOT NULL,
    phone                 VARCHAR(20),
    is_enabled            BOOLEAN      NOT NULL DEFAULT TRUE,
    is_locked             BOOLEAN      NOT NULL DEFAULT FALSE,
    failed_login_attempts INT          NOT NULL DEFAULT 0,
    locked_until          TIMESTAMPTZ,
    last_login_at         TIMESTAMPTZ,
    totp_secret           TEXT,                          -- encrypted TOTP seed
    totp_enabled          BOOLEAN      NOT NULL DEFAULT FALSE,
    must_change_password  BOOLEAN      NOT NULL DEFAULT FALSE,
    is_deleted            BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by            UUID,
    updated_by            UUID
);

CREATE UNIQUE INDEX idx_users_username ON users(username) WHERE is_deleted = FALSE;
CREATE UNIQUE INDEX idx_users_email    ON users(email)    WHERE is_deleted = FALSE;
```

**Business rules:**
- `username` and `email` must be unique among non-deleted users
- `password_hash` stores BCrypt output — never plain text
- `failed_login_attempts` resets to 0 on successful login
- `locked_until` is checked at login; if now() > locked_until, the lock is cleared automatically

---

### 5.2 `role_assignments`

```sql
CREATE TABLE role_assignments (
    id            UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id       UUID         NOT NULL REFERENCES users(id),
    role          VARCHAR(50)  NOT NULL,       -- enum: SUPER_ADMIN, ADMIN, HOD, FACULTY, etc.
    department_id UUID,                         -- optional scope: limits HOD to one department
    assigned_by   UUID         NOT NULL,       -- who created this assignment
    assigned_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expires_at    TIMESTAMPTZ,                  -- NULL = permanent
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    UNIQUE (user_id, role, department_id)
);

CREATE INDEX idx_role_assignment_user ON role_assignments(user_id);
CREATE INDEX idx_role_assignment_role ON role_assignments(role);
```

**Business rules:**
- Active assignments are: `is_active = TRUE AND (expires_at IS NULL OR expires_at > NOW())`
- A user may hold multiple roles (e.g., a person who is both HOD and Faculty)
- `department_id` scoping is used for HOD role — the HOD can only manage their department

---

### 5.3 `permission_grants`

```sql
CREATE TABLE permission_grants (
    id                  UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    grantee_user_id     UUID          NOT NULL REFERENCES users(id),
    granted_by_user_id  UUID          NOT NULL REFERENCES users(id),
    permission_key      VARCHAR(100)  NOT NULL,   -- e.g. 'FINANCE.FEE_COLLECTION'
    scope_department_id UUID,
    scope_batch_id      UUID,
    scope_fee_head      VARCHAR(100),
    amount_threshold    NUMERIC(14,2),
    granted_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    effective_from      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    effective_until     TIMESTAMPTZ,              -- NULL = permanent until revoked
    revoked_at          TIMESTAMPTZ,
    revoked_by_user_id  UUID
);

CREATE INDEX idx_pg_grantee ON permission_grants(grantee_user_id);
```

---

### 5.4 `refresh_tokens`

```sql
CREATE TABLE refresh_tokens (
    id           UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id      UUID         NOT NULL REFERENCES users(id),
    token_hash   VARCHAR(64)  NOT NULL UNIQUE,  -- SHA-256 hex of raw token
    device_info  VARCHAR(255),
    ip_address   VARCHAR(45),
    expires_at   TIMESTAMPTZ  NOT NULL,
    revoked_at   TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_token_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_token_user ON refresh_tokens(user_id);
```

---

## 6. Module: Academic

### 6.1 `departments`

```sql
CREATE TABLE departments (
    id             UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name           VARCHAR(150) NOT NULL,
    code           VARCHAR(20)  NOT NULL,    -- unique among non-deleted
    description    TEXT,
    hod_user_id    UUID,                     -- current HOD (FK to users.id)
    contact_email  VARCHAR(150),
    contact_phone  VARCHAR(20),
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    -- base columns
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);

CREATE UNIQUE INDEX idx_dept_code ON departments(code) WHERE is_deleted = FALSE;
```

---

### 6.2 `academic_years`

```sql
CREATE TABLE academic_years (
    id         UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    label      VARCHAR(20) NOT NULL UNIQUE,   -- e.g. '2024-2025'
    start_year INT         NOT NULL,
    end_year   INT         NOT NULL,
    is_current BOOLEAN     NOT NULL DEFAULT FALSE,
    -- base columns
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);
-- Only one row may have is_current = TRUE (enforced via application logic + index)
CREATE UNIQUE INDEX idx_acyr_current ON academic_years(is_current) WHERE is_current = TRUE;
```

---

### 6.3 `courses`

```sql
CREATE TABLE courses (
    id               UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    department_id    UUID         NOT NULL REFERENCES departments(id),
    name             VARCHAR(200) NOT NULL,
    code             VARCHAR(20)  NOT NULL,   -- unique among non-deleted
    level            VARCHAR(20)  NOT NULL,   -- UG | PG | DIPLOMA | PHD | CERTIFICATE
    duration_years   INT          NOT NULL,
    total_credits    INT          NOT NULL DEFAULT 0,
    intake_capacity  INT          NOT NULL,
    description      TEXT,
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);

CREATE UNIQUE INDEX idx_course_code ON courses(code) WHERE is_deleted = FALSE;
CREATE INDEX idx_course_dept ON courses(department_id);
```

---

### 6.4 `terms`

```sql
CREATE TABLE terms (
    id               UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    academic_year_id UUID        NOT NULL REFERENCES academic_years(id),
    name             VARCHAR(50) NOT NULL,    -- e.g. 'Semester I'
    term_number      INT         NOT NULL,
    start_date       DATE        NOT NULL,
    end_date         DATE        NOT NULL,
    exam_start_date  DATE,
    exam_end_date    DATE,
    is_active        BOOLEAN     NOT NULL DEFAULT FALSE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);
```

---

### 6.5 `batches`

```sql
CREATE TABLE batches (
    id                  UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    course_id           UUID         NOT NULL REFERENCES courses(id),
    academic_year_id    UUID         NOT NULL REFERENCES academic_years(id),
    label               VARCHAR(100) NOT NULL,
    intake_year         INT          NOT NULL,
    current_term_number INT          NOT NULL DEFAULT 1,
    is_active           BOOLEAN      NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID,
    UNIQUE (course_id, academic_year_id)
);

CREATE INDEX idx_batch_course ON batches(course_id);
```

---

### 6.6 `sections`

```sql
CREATE TABLE sections (
    id        UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    batch_id  UUID        NOT NULL REFERENCES batches(id),
    name      VARCHAR(10) NOT NULL,
    capacity  INT         NOT NULL,
    is_active BOOLEAN     NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID,
    UNIQUE (batch_id, name)
);
```

---

### 6.7 `subjects`

```sql
CREATE TABLE subjects (
    id            UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    code          VARCHAR(20)  NOT NULL,   -- unique among non-deleted
    name          VARCHAR(200) NOT NULL,
    department_id UUID         NOT NULL REFERENCES departments(id),
    credits       INT          NOT NULL DEFAULT 0,
    type          VARCHAR(20)  NOT NULL,   -- THEORY | LAB | PROJECT | ELECTIVE | SEMINAR
    description   TEXT,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);

CREATE UNIQUE INDEX idx_subject_code ON subjects(code) WHERE is_deleted = FALSE;
```

---

### 6.8 `subject_offerings`

```sql
CREATE TABLE subject_offerings (
    id                  UUID    NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    batch_id            UUID    NOT NULL REFERENCES batches(id),
    subject_id          UUID    NOT NULL REFERENCES subjects(id),
    term_id             UUID    NOT NULL REFERENCES terms(id),
    max_internal_marks  INT     NOT NULL DEFAULT 40,
    max_external_marks  INT     NOT NULL DEFAULT 60,
    pass_marks_internal INT     NOT NULL DEFAULT 16,
    pass_marks_external INT     NOT NULL DEFAULT 24,
    is_elective         BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID,
    UNIQUE (batch_id, subject_id, term_id)
);
```

---

### 6.9 `faculty_subject_mappings`

```sql
CREATE TABLE faculty_subject_mappings (
    id              UUID    NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    offering_id     UUID    NOT NULL REFERENCES subject_offerings(id),
    section_id      UUID    NOT NULL REFERENCES sections(id),
    faculty_user_id UUID    NOT NULL REFERENCES users(id),
    is_primary      BOOLEAN NOT NULL DEFAULT TRUE,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID,
    UNIQUE (offering_id, section_id, faculty_user_id)
);
```

---

## 7. Module: Student

### 7.1 `students`

```sql
CREATE TABLE students (
    id                UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id           UUID         UNIQUE REFERENCES users(id),  -- system login account
    enrollment_number VARCHAR(30)  NOT NULL UNIQUE,
    first_name        VARCHAR(100) NOT NULL,
    last_name         VARCHAR(100) NOT NULL,
    personal_email    VARCHAR(150) NOT NULL,
    phone             VARCHAR(20),
    date_of_birth     DATE         NOT NULL,
    gender            VARCHAR(20)  NOT NULL,   -- MALE | FEMALE | OTHER | PREFER_NOT_TO_SAY
    blood_group       VARCHAR(5),
    aadhar_number     VARCHAR(12),             -- stored, masked on retrieval
    category          VARCHAR(30),             -- GENERAL | OBC | SC | ST | EWS
    nationality       VARCHAR(50)  NOT NULL DEFAULT 'Indian',
    address           TEXT,
    city              VARCHAR(100),
    state             VARCHAR(100),
    pincode           VARCHAR(10),
    photo_file_id     VARCHAR(36),
    status            VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    -- ACTIVE | DETAINED | DISCONTINUED | GRADUATED | ON_LEAVE | TRANSFERRED
    admission_date    DATE         NOT NULL,
    graduation_date   DATE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);

CREATE UNIQUE INDEX idx_student_enrollment ON students(enrollment_number) WHERE is_deleted = FALSE;
CREATE INDEX idx_student_email ON students(personal_email) WHERE is_deleted = FALSE;
```

---

### 7.2 `enrollments` (append-only by design)

```sql
CREATE TABLE enrollments (
    id              UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    student_id      UUID        NOT NULL REFERENCES students(id),
    batch_id        UUID        NOT NULL REFERENCES batches(id),
    section_id      UUID        NOT NULL REFERENCES sections(id),
    term_id         UUID        NOT NULL REFERENCES terms(id),
    enrollment_date DATE        NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    -- ACTIVE | PROMOTED | DETAINED | TRANSFERRED | DISCONTINUED
    is_current      BOOLEAN     NOT NULL DEFAULT TRUE,  -- only one row per student is TRUE
    remarks         TEXT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);

CREATE INDEX idx_enrollment_student ON enrollments(student_id);
CREATE INDEX idx_enrollment_section ON enrollments(section_id);
CREATE INDEX idx_enrollment_current ON enrollments(student_id, is_current);
```

**Key rule:** Before inserting a new enrollment, update all existing rows for this student:  
`UPDATE enrollments SET is_current = FALSE WHERE student_id = ? AND is_current = TRUE`

---

### 7.3 `guardians`

```sql
CREATE TABLE guardians (
    id            UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    student_id    UUID         NOT NULL REFERENCES students(id),
    name          VARCHAR(150) NOT NULL,
    relationship  VARCHAR(30)  NOT NULL,  -- FATHER | MOTHER | GUARDIAN
    phone         VARCHAR(20)  NOT NULL,
    email         VARCHAR(150),
    occupation    VARCHAR(100),
    annual_income BIGINT,
    is_primary    BOOLEAN      NOT NULL DEFAULT FALSE,
    user_id       UUID         REFERENCES users(id),  -- parent portal login
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);

CREATE INDEX idx_guardian_student ON guardians(student_id);
```

---

## 8. Module: Attendance

### 8.1 `attendance_policies`

```sql
CREATE TABLE attendance_policies (
    id                        UUID  NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    course_id                 UUID  NOT NULL UNIQUE,
    min_attendance_pct        INT   NOT NULL DEFAULT 75,
    medical_condonation_pct   INT   NOT NULL DEFAULT 65,
    consecutive_absence_alert INT   NOT NULL DEFAULT 3,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);
```

---

### 8.2 `attendance_sessions`

```sql
CREATE TABLE attendance_sessions (
    id              UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    offering_id     UUID        NOT NULL REFERENCES subject_offerings(id),
    section_id      UUID        NOT NULL REFERENCES sections(id),
    faculty_user_id UUID        NOT NULL REFERENCES users(id),
    session_date    DATE        NOT NULL,
    start_time      TIME,
    end_time        TIME,
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT',   -- DRAFT | SUBMITTED | LOCKED
    remarks         TEXT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID,
    UNIQUE (offering_id, section_id, session_date)
);

CREATE INDEX idx_att_session_offering ON attendance_sessions(offering_id);
CREATE INDEX idx_att_session_section  ON attendance_sessions(section_id);
CREATE INDEX idx_att_session_date     ON attendance_sessions(session_date);
```

---

### 8.3 `attendance_records`

```sql
CREATE TABLE attendance_records (
    id         UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    session_id UUID        NOT NULL REFERENCES attendance_sessions(id),
    student_id UUID        NOT NULL REFERENCES students(id),
    status     VARCHAR(20) NOT NULL,   -- PRESENT | ABSENT | LATE | EXCUSED | HOLIDAY
    remarks    VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID,
    UNIQUE (session_id, student_id)
);

CREATE INDEX idx_att_record_session ON attendance_records(session_id);
CREATE INDEX idx_att_record_student ON attendance_records(student_id);
```

---

### 8.4 `condonation_requests`

```sql
CREATE TABLE condonation_requests (
    id                  UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    student_id          UUID        NOT NULL REFERENCES students(id),
    offering_id         UUID        NOT NULL REFERENCES subject_offerings(id),
    term_id             UUID        NOT NULL REFERENCES terms(id),
    reason              TEXT        NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING | APPROVED | REJECTED
    reviewed_by_user_id UUID        REFERENCES users(id),
    review_remarks      TEXT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);

CREATE INDEX idx_cond_student ON condonation_requests(student_id);
```

---

## 9. Module: Examination

### 9.1 `exams`

```sql
CREATE TABLE exams (
    id          UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    exam_type   VARCHAR(30)  NOT NULL,
    -- INTERNAL_1 | INTERNAL_2 | MID_TERM | END_TERM | PRACTICAL | VIVA | PROJECT
    batch_id    UUID         NOT NULL REFERENCES batches(id),
    term_id     UUID         NOT NULL REFERENCES terms(id),
    start_date  DATE         NOT NULL,
    end_date    DATE         NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'SCHEDULED',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);
```

---

### 9.2 `hall_tickets`

```sql
CREATE TABLE hall_tickets (
    id                  UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    exam_id             UUID        NOT NULL REFERENCES exams(id),
    student_id          UUID        NOT NULL REFERENCES students(id),
    hall_ticket_number  VARCHAR(30) NOT NULL UNIQUE,
    is_eligible         BOOLEAN     NOT NULL DEFAULT TRUE,
    ineligibility_reason TEXT,
    issued_at           TIMESTAMPTZ,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID,
    UNIQUE (exam_id, student_id)
);

CREATE INDEX idx_hall_ticket_exam    ON hall_tickets(exam_id);
CREATE INDEX idx_hall_ticket_student ON hall_tickets(student_id);
```

---

### 9.3 `marks_entries`

```sql
CREATE TABLE marks_entries (
    id              UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    exam_id         UUID          NOT NULL REFERENCES exams(id),
    offering_id     UUID          NOT NULL REFERENCES subject_offerings(id),
    student_id      UUID          NOT NULL REFERENCES students(id),
    internal_marks  NUMERIC(6,2),
    external_marks  NUMERIC(6,2),
    total_marks     NUMERIC(6,2),
    is_pass         BOOLEAN,
    grade           VARCHAR(5),
    grade_points    NUMERIC(4,2),
    status          VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    -- DRAFT | SUBMITTED | MODERATED | PUBLISHED
    entered_by      UUID          REFERENCES users(id),
    moderated_by    UUID          REFERENCES users(id),
    moderated_at    TIMESTAMPTZ,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID,
    UNIQUE (exam_id, offering_id, student_id)
);

CREATE INDEX idx_marks_student ON marks_entries(student_id);
CREATE INDEX idx_marks_exam    ON marks_entries(exam_id);
```

---

### 9.4 `result_records`

```sql
CREATE TABLE result_records (
    id                   UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    student_id           UUID          NOT NULL REFERENCES students(id),
    batch_id             UUID          NOT NULL REFERENCES batches(id),
    term_id              UUID          NOT NULL REFERENCES terms(id),
    sgpa                 NUMERIC(4,2),
    cgpa                 NUMERIC(4,2),
    total_credits_earned INT,
    result_status        VARCHAR(20)   NOT NULL,  -- PASS | FAIL | WITHHELD | DETAINED
    published_at         TIMESTAMPTZ,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID,
    UNIQUE (student_id, term_id)
);
```

---

### 9.5 `revaluation_requests`

```sql
CREATE TABLE revaluation_requests (
    id             UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    marks_entry_id UUID        NOT NULL REFERENCES marks_entries(id),
    student_id     UUID        NOT NULL REFERENCES students(id),
    reason         TEXT,
    fee_payment_id UUID,                    -- optional: revaluation fee
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewed_marks NUMERIC(6,2),
    reviewed_by    UUID        REFERENCES users(id),
    reviewed_at    TIMESTAMPTZ,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);
```

---

## 10. Module: Finance

### 10.1 `financial_years`

```sql
CREATE TABLE financial_years (
    id                  UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    label               VARCHAR(20) NOT NULL UNIQUE,
    start_date          DATE        NOT NULL,
    end_date            DATE        NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    -- OPEN | CLOSING_PENDING | CLOSED
    closed_by_user_id   UUID        REFERENCES users(id),
    approved_by_user_id UUID        REFERENCES users(id),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);
```

---

### 10.2 `account_heads`

```sql
CREATE TABLE account_heads (
    id          UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    code        VARCHAR(30)  NOT NULL UNIQUE,
    name        VARCHAR(150) NOT NULL,
    description TEXT,
    type        VARCHAR(20)  NOT NULL,  -- INCOME | EXPENSE | ASSET | LIABILITY
    parent_id   UUID         REFERENCES account_heads(id),
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);
```

---

### 10.3 `fee_heads`

```sql
CREATE TABLE fee_heads (
    id          UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    code        VARCHAR(30)  NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);
```

---

### 10.4 `fee_structures`

```sql
CREATE TABLE fee_structures (
    id                   UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    course_id            UUID          NOT NULL REFERENCES courses(id),
    batch_id             UUID,                   -- NULL = applies to all batches of this course
    fee_head_id          UUID          NOT NULL REFERENCES fee_heads(id),
    term_number          INT           NOT NULL,
    amount               NUMERIC(14,2) NOT NULL,
    due_date_offset_days INT,                    -- days after term start date
    late_fee_per_day     NUMERIC(10,2),
    is_active            BOOLEAN       NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID,
    UNIQUE (course_id, batch_id, fee_head_id, term_number)
);
```

---

### 10.5 `fee_invoices`

```sql
CREATE TABLE fee_invoices (
    id               UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    invoice_number   VARCHAR(30)   NOT NULL UNIQUE,
    student_id       UUID          NOT NULL REFERENCES students(id),
    course_id        UUID          NOT NULL REFERENCES courses(id),
    batch_id         UUID          NOT NULL REFERENCES batches(id),
    term_number      INT           NOT NULL,
    academic_year_id UUID          NOT NULL REFERENCES academic_years(id),
    total_amount     NUMERIC(14,2) NOT NULL,
    discount_amount  NUMERIC(14,2) NOT NULL DEFAULT 0,
    late_fee         NUMERIC(14,2) NOT NULL DEFAULT 0,
    net_amount       NUMERIC(14,2) NOT NULL,
    paid_amount      NUMERIC(14,2) NOT NULL DEFAULT 0,
    due_date         DATE          NOT NULL,
    status           VARCHAR(20)   NOT NULL DEFAULT 'UNPAID',
    -- UNPAID | PARTIAL | PAID | OVERDUE | WAIVED | CANCELLED
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);

CREATE INDEX idx_invoice_student ON fee_invoices(student_id);
CREATE UNIQUE INDEX idx_invoice_number ON fee_invoices(invoice_number);
```

---

### 10.6 `fee_payments`

```sql
CREATE TABLE fee_payments (
    id                    UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    invoice_id            UUID          NOT NULL REFERENCES fee_invoices(id),
    receipt_number        VARCHAR(30)   NOT NULL UNIQUE,
    amount                NUMERIC(14,2) NOT NULL,
    payment_mode          VARCHAR(30)   NOT NULL,
    -- CASH | CHEQUE | DEMAND_DRAFT | ONLINE_UPI | ONLINE_CARD | ONLINE_NET_BANKING | NEFT | RTGS
    transaction_reference VARCHAR(100),
    payment_gateway       VARCHAR(50),
    collected_by_user_id  UUID          REFERENCES users(id),
    payment_timestamp     TIMESTAMPTZ   NOT NULL,
    status                VARCHAR(20)   NOT NULL DEFAULT 'SUCCESS',
    notes                 TEXT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);

CREATE INDEX idx_payment_invoice ON fee_payments(invoice_id);
```

---

### 10.7 `ledger_entries` (append-only)

```sql
CREATE TABLE ledger_entries (
    id                UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    account_head_id   UUID          NOT NULL REFERENCES account_heads(id),
    description       TEXT          NOT NULL,
    entry_type        VARCHAR(10)   NOT NULL,  -- DEBIT | CREDIT
    amount            NUMERIC(14,2) NOT NULL,
    entry_date        DATE          NOT NULL,
    financial_year_id UUID          NOT NULL REFERENCES financial_years(id),
    reference_type    VARCHAR(50),             -- FEE_PAYMENT | SALARY | VENDOR_PAYMENT
    reference_id      VARCHAR(36),             -- UUID of the originating record
    department_id     UUID,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by        UUID
    -- NO is_deleted, updated_at, updated_by — this table is immutable
);

CREATE INDEX idx_ledger_account ON ledger_entries(account_head_id);
CREATE INDEX idx_ledger_date    ON ledger_entries(entry_date DESC);
CREATE INDEX idx_ledger_year    ON ledger_entries(financial_year_id);
```

**Triggers (see Section 17):** Two triggers prevent UPDATE and DELETE.

---

### 10.8 `accountant_permission_grants`

```sql
CREATE TABLE accountant_permission_grants (
    id                  UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    grantee_user_id     UUID          NOT NULL REFERENCES users(id),
    granted_by_user_id  UUID          NOT NULL REFERENCES users(id),
    permission_key      VARCHAR(100)  NOT NULL,
    scope_department_id UUID,
    scope_batch_id      UUID,
    scope_fee_head_code VARCHAR(30),
    amount_threshold    NUMERIC(14,2),
    effective_from      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    effective_until     TIMESTAMPTZ,
    revoked_at          TIMESTAMPTZ,
    revoked_by_user_id  UUID          REFERENCES users(id),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);

CREATE INDEX idx_apg_grantee ON accountant_permission_grants(grantee_user_id);
```

---

### 10.9 `vendors` and `purchase_orders`

```sql
CREATE TABLE vendors (
    id              UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    code            VARCHAR(20)  NOT NULL UNIQUE,
    name            VARCHAR(200) NOT NULL,
    contact_person  VARCHAR(100),
    email           VARCHAR(150),
    phone           VARCHAR(20),
    address         TEXT,
    gst_number      VARCHAR(20),
    pan_number      VARCHAR(10),
    bank_account_number VARCHAR(30),
    ifsc_code       VARCHAR(11),
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);

CREATE TABLE purchase_orders (
    id                     UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    po_number              VARCHAR(30)   NOT NULL UNIQUE,
    vendor_id              UUID          NOT NULL REFERENCES vendors(id),
    department_id          UUID,
    financial_year_id      UUID          NOT NULL REFERENCES financial_years(id),
    total_amount           NUMERIC(14,2) NOT NULL,
    description            TEXT,
    po_date                DATE          NOT NULL,
    expected_delivery_date DATE,
    status                 VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    -- DRAFT | PENDING_APPROVAL | APPROVED | RECEIVED | CANCELLED
    approved_by_user_id    UUID          REFERENCES users(id),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);
```

---

## 11. Module: Library

### 11.1 `loan_policies`

```sql
CREATE TABLE loan_policies (
    id               UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    borrower_type    VARCHAR(30)   NOT NULL UNIQUE,  -- STUDENT | FACULTY | STAFF
    max_books        INT           NOT NULL DEFAULT 3,
    loan_days        INT           NOT NULL DEFAULT 14,
    renewals_allowed INT           NOT NULL DEFAULT 2,
    fine_per_day     NUMERIC(8,2)  NOT NULL DEFAULT 2.00,
    max_fine         NUMERIC(10,2)          DEFAULT 200.00,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);
```

---

### 11.2 `book_titles`

```sql
CREATE TABLE book_titles (
    id                     UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    title                  TEXT         NOT NULL,
    author                 VARCHAR(300) NOT NULL,
    isbn                   VARCHAR(20),
    edition                VARCHAR(50),
    publisher              VARCHAR(200),
    publication_year       INT,
    subject_classification VARCHAR(100),
    language               VARCHAR(30)  NOT NULL DEFAULT 'English',
    cover_image_file_id    VARCHAR(36),
    description            TEXT,
    department_id          UUID         REFERENCES departments(id),
    total_copies           INT          NOT NULL DEFAULT 0,
    available_copies       INT          NOT NULL DEFAULT 0,
    is_active              BOOLEAN      NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);

CREATE INDEX idx_book_isbn    ON book_titles(isbn)                    WHERE is_deleted = FALSE;
CREATE INDEX idx_book_subject ON book_titles(subject_classification)   WHERE is_deleted = FALSE;
```

---

### 11.3 `book_copies`

```sql
CREATE TABLE book_copies (
    id                    UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    title_id              UUID        NOT NULL REFERENCES book_titles(id),
    barcode               VARCHAR(50) NOT NULL UNIQUE,
    accession_number      VARCHAR(30) NOT NULL UNIQUE,
    shelf_location        VARCHAR(50),
    rack_number           VARCHAR(20),
    condition             VARCHAR(20) NOT NULL DEFAULT 'GOOD',
    -- EXCELLENT | GOOD | FAIR | POOR | DAMAGED | LOST
    status                VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    -- AVAILABLE | ISSUED | RESERVED | LOST | RETIRED
    barcode_image_file_id VARCHAR(36),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);

CREATE UNIQUE INDEX idx_copy_barcode ON book_copies(barcode)  WHERE is_deleted = FALSE;
CREATE INDEX idx_copy_title          ON book_copies(title_id) WHERE is_deleted = FALSE;
```

---

### 11.4 `library_custody_log` (append-only)

```sql
CREATE TABLE library_custody_log (
    id                   UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    copy_id              UUID          NOT NULL REFERENCES book_copies(id),
    borrower_user_id     UUID          NOT NULL REFERENCES users(id),
    borrower_name        VARCHAR(200)  NOT NULL,
    borrower_type        VARCHAR(20)   NOT NULL,  -- STUDENT | FACULTY | STAFF
    issued_by_user_id    UUID          NOT NULL REFERENCES users(id),
    returned_by_user_id  UUID          REFERENCES users(id),
    issued_at            TIMESTAMPTZ   NOT NULL,
    due_at               TIMESTAMPTZ   NOT NULL,
    returned_at          TIMESTAMPTZ,             -- NULL until returned
    condition_on_issue   VARCHAR(20),
    condition_on_return  VARCHAR(20),
    fine_amount          NUMERIC(10,2),
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW()
    -- NO is_deleted, updated_at, updated_by — partial immutability via trigger
);

CREATE INDEX idx_custody_copy     ON library_custody_log(copy_id);
CREATE INDEX idx_custody_borrower ON library_custody_log(borrower_user_id);
CREATE INDEX idx_custody_overdue  ON library_custody_log(due_at)
    WHERE returned_at IS NULL;  -- partial index for overdue queries
```

---

### 11.5 `reservations`

```sql
CREATE TABLE reservations (
    id             UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    title_id       UUID        NOT NULL REFERENCES book_titles(id),
    user_id        UUID        NOT NULL REFERENCES users(id),
    queue_position INT         NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    -- WAITING | READY | FULFILLED | EXPIRED | CANCELLED
    notified_at    TIMESTAMPTZ,
    expires_at     TIMESTAMPTZ,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);

CREATE INDEX idx_reservation_title ON reservations(title_id);
```

---

### 11.6 `library_fines`

```sql
CREATE TABLE library_fines (
    id                UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id           UUID          NOT NULL REFERENCES users(id),
    custody_log_id    UUID          NOT NULL REFERENCES library_custody_log(id),
    amount            NUMERIC(10,2) NOT NULL,
    paid_amount       NUMERIC(10,2) NOT NULL DEFAULT 0,
    status            VARCHAR(20)   NOT NULL DEFAULT 'UNPAID',
    -- UNPAID | PARTIAL | PAID | WAIVED
    waived_by_user_id UUID          REFERENCES users(id),
    waiver_reason     TEXT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);

CREATE INDEX idx_fine_user ON library_fines(user_id);
```

---

## 12. Module: Asset

### 12.1 `asset_categories`

```sql
CREATE TABLE asset_categories (
    id                    UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name                  VARCHAR(100) NOT NULL,
    depreciation_rate_pct INT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);
```

---

### 12.2 `assets`

```sql
CREATE TABLE assets (
    id                    UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    asset_tag             VARCHAR(50)   NOT NULL UNIQUE,
    name                  VARCHAR(200)  NOT NULL,
    description           TEXT,
    category_id           UUID          REFERENCES asset_categories(id),
    department_id         UUID          NOT NULL,
    room_location         VARCHAR(100),
    purchase_date         DATE,
    purchase_cost         NUMERIC(14,2),
    vendor_id             UUID          REFERENCES vendors(id),
    warranty_expiry       DATE,
    assigned_to_user_id   UUID          REFERENCES users(id),
    status                VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    -- ACTIVE | UNDER_MAINTENANCE | DISPOSED | LOST | TRANSFERRED
    barcode_image_file_id VARCHAR(36),
    photo_file_id         VARCHAR(36),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);

CREATE UNIQUE INDEX idx_asset_tag  ON assets(asset_tag)     WHERE is_deleted = FALSE;
CREATE INDEX idx_asset_dept        ON assets(department_id) WHERE is_deleted = FALSE;
```

---

### 12.3 `asset_custody_log` (append-only)

```sql
CREATE TABLE asset_custody_log (
    id                     UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    asset_id               UUID        NOT NULL REFERENCES assets(id),
    from_department_id     UUID,
    to_department_id       UUID        NOT NULL,
    from_user_id           UUID        REFERENCES users(id),
    assigned_to_user_id    UUID        REFERENCES users(id),
    transferred_by_user_id UUID        NOT NULL REFERENCES users(id),
    transferred_at         TIMESTAMPTZ NOT NULL,
    reason                 TEXT,
    from_location          VARCHAR(100),
    to_location            VARCHAR(100),
    created_at             TIMESTAMPTZ NOT NULL DEFAULT NOW()
    -- NO is_deleted, updated_at — fully immutable via trigger
);

CREATE INDEX idx_acl_asset    ON asset_custody_log(asset_id);
CREATE INDEX idx_acl_assignee ON asset_custody_log(assigned_to_user_id);
```

---

### 12.4 `maintenance_tickets`

```sql
CREATE TABLE maintenance_tickets (
    id           UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    asset_id     UUID         NOT NULL REFERENCES assets(id),
    reported_by  UUID         NOT NULL REFERENCES users(id),
    title        VARCHAR(200) NOT NULL,
    description  TEXT,
    priority     VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',
    -- LOW | MEDIUM | HIGH | CRITICAL
    status       VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    -- OPEN | ASSIGNED | IN_PROGRESS | RESOLVED | CLOSED | CANCELLED
    assigned_to  UUID         REFERENCES users(id),
    resolved_at  TIMESTAMPTZ,
    resolution   TEXT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);

CREATE INDEX idx_mt_asset  ON maintenance_tickets(asset_id);
CREATE INDEX idx_mt_status ON maintenance_tickets(status);
```

---

## 13. Module: HR

### 13.1 `designations`

```sql
CREATE TABLE designations (
    id         UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    grade      VARCHAR(20),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);
```

---

### 13.2 `staff_profiles`

```sql
CREATE TABLE staff_profiles (
    id                  UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id             UUID        NOT NULL UNIQUE REFERENCES users(id),
    employee_number     VARCHAR(20) NOT NULL UNIQUE,
    department_id       UUID        NOT NULL REFERENCES departments(id),
    designation_id      UUID        REFERENCES designations(id),
    staff_type          VARCHAR(20) NOT NULL,  -- TEACHING | NON_TEACHING
    joining_date        DATE        NOT NULL,
    qualification       VARCHAR(200),
    specialization      TEXT,
    experience_years    INT,
    bank_account_number VARCHAR(30),
    ifsc_code           VARCHAR(11),
    pan_number          VARCHAR(10),
    pf_number           VARCHAR(20),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);
```

---

### 13.3 `leave_types`, `leave_balances`, `leave_requests`

```sql
CREATE TABLE leave_types (
    id             UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    code           VARCHAR(10) NOT NULL UNIQUE,
    name           VARCHAR(100) NOT NULL,
    days_per_year  INT         NOT NULL,
    is_paid        BOOLEAN     NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);

CREATE TABLE leave_balances (
    id             UUID NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id        UUID NOT NULL REFERENCES users(id),
    leave_type_id  UUID NOT NULL REFERENCES leave_types(id),
    year           INT  NOT NULL,
    total_days     INT  NOT NULL DEFAULT 0,
    used_days      INT  NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, leave_type_id, year)
);

CREATE TABLE leave_requests (
    id               UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id          UUID        NOT NULL REFERENCES users(id),
    leave_type_id    UUID        NOT NULL REFERENCES leave_types(id),
    from_date        DATE        NOT NULL,
    to_date          DATE        NOT NULL,
    days_requested   INT         NOT NULL,
    reason           TEXT,
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    -- PENDING | APPROVED | REJECTED | CANCELLED
    reviewed_by      UUID        REFERENCES users(id),
    reviewed_at      TIMESTAMPTZ,
    review_remarks   TEXT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);

CREATE INDEX idx_leave_user ON leave_requests(user_id);
```

---

## 14. Module: Hostel & Transport

### 14.1 Hostel Tables

```sql
CREATE TABLE hostel_blocks (
    id         UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    gender     VARCHAR(10)  NOT NULL,  -- MALE | FEMALE | MIXED
    warden_id  UUID         REFERENCES users(id),
    capacity   INT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);

CREATE TABLE rooms (
    id          UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    block_id    UUID        NOT NULL REFERENCES hostel_blocks(id),
    room_number VARCHAR(20) NOT NULL,
    floor       INT,
    capacity    INT         NOT NULL DEFAULT 2,
    room_type   VARCHAR(20),             -- SINGLE | DOUBLE | TRIPLE
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID,
    UNIQUE (block_id, room_number)
);

CREATE TABLE room_allocations (
    id         UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    room_id    UUID        NOT NULL REFERENCES rooms(id),
    student_id UUID        NOT NULL REFERENCES students(id),
    from_date  DATE        NOT NULL,
    to_date    DATE,
    status     VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);

CREATE TABLE outpass_requests (
    id            UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    student_id    UUID        NOT NULL REFERENCES students(id),
    from_datetime TIMESTAMPTZ NOT NULL,
    to_datetime   TIMESTAMPTZ NOT NULL,
    reason        TEXT        NOT NULL,
    destination   VARCHAR(200),
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approved_by   UUID        REFERENCES users(id),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);
```

---

### 14.2 Transport Tables

```sql
CREATE TABLE routes (
    id          UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);

CREATE TABLE vehicles (
    id              UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    registration_no VARCHAR(20) NOT NULL UNIQUE,
    vehicle_type    VARCHAR(30),
    capacity        INT,
    driver_name     VARCHAR(100),
    driver_phone    VARCHAR(20),
    driver_license  VARCHAR(30),
    license_expiry  DATE,
    is_active       BOOLEAN     NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);

CREATE TABLE student_route_mappings (
    id         UUID    NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    student_id UUID    NOT NULL REFERENCES students(id),
    route_id   UUID    NOT NULL REFERENCES routes(id),
    vehicle_id UUID    REFERENCES vehicles(id),
    stop_name  VARCHAR(100),
    is_active  BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);
```

---

## 15. Module: Communication

```sql
CREATE TABLE notices (
    id           UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    title        VARCHAR(300) NOT NULL,
    body         TEXT         NOT NULL,
    target_type  VARCHAR(20)  NOT NULL DEFAULT 'ALL',
    -- ALL | DEPARTMENT | BATCH | ROLE
    target_id    UUID,                       -- department_id or batch_id when targeted
    is_pinned    BOOLEAN      NOT NULL DEFAULT FALSE,
    published_at TIMESTAMPTZ,
    expires_at   TIMESTAMPTZ,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);

CREATE TABLE helpdesk_tickets (
    id                UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    raised_by_user_id UUID         NOT NULL REFERENCES users(id),
    title             VARCHAR(300) NOT NULL,
    description       TEXT         NOT NULL,
    category          VARCHAR(50)  NOT NULL,  -- ACADEMIC | FINANCIAL | IT | FACILITIES
    priority          VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',
    status            VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    assigned_to       UUID         REFERENCES users(id),
    resolved_at       TIMESTAMPTZ,
    resolution        TEXT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);

CREATE INDEX idx_ticket_user   ON helpdesk_tickets(raised_by_user_id);
CREATE INDEX idx_ticket_status ON helpdesk_tickets(status);

CREATE TABLE ticket_comments (
    id         UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    ticket_id  UUID        NOT NULL REFERENCES helpdesk_tickets(id),
    user_id    UUID        NOT NULL REFERENCES users(id),
    comment    TEXT        NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);
```

---

## 16. Module: Notifications & Scheduler

```sql
CREATE TABLE notification_templates (
    id           UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    template_key VARCHAR(100) NOT NULL UNIQUE,
    subject      VARCHAR(300),
    body_text    TEXT         NOT NULL,
    body_html    TEXT,
    channels     VARCHAR(100),  -- comma-separated: EMAIL,SMS,IN_APP
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);

CREATE TABLE notification_records (
    id              UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id         UUID         NOT NULL REFERENCES users(id),
    template_key    VARCHAR(100),
    title           VARCHAR(300) NOT NULL,
    body            TEXT         NOT NULL,
    channel         VARCHAR(20)  NOT NULL DEFAULT 'IN_APP',
    is_read         BOOLEAN      NOT NULL DEFAULT FALSE,
    read_at         TIMESTAMPTZ,
    sent_at         TIMESTAMPTZ,
    reference_type  VARCHAR(50),
    reference_id    VARCHAR(36),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notification_user   ON notification_records(user_id);
CREATE INDEX idx_notification_unread ON notification_records(user_id, is_read)
    WHERE is_read = FALSE;

CREATE TABLE background_jobs (
    id             UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    job_type       VARCHAR(100) NOT NULL,
    payload        JSONB        NOT NULL DEFAULT '{}',
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    -- PENDING | RUNNING | DONE | FAILED
    attempts       INT          NOT NULL DEFAULT 0,
    max_attempts   INT          NOT NULL DEFAULT 3,
    scheduled_for  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    started_at     TIMESTAMPTZ,
    completed_at   TIMESTAMPTZ,
    last_error     TEXT,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_bg_job_pending ON background_jobs(status, scheduled_for)
    WHERE status = 'PENDING';
```

---

## 17. Append-Only Tables and Triggers

### audit_log — Fully Immutable

```sql
CREATE TABLE audit_log (
    id           UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id      UUID,
    username     VARCHAR(100),
    action       VARCHAR(50)  NOT NULL,
    module       VARCHAR(50),
    entity_type  VARCHAR(100) NOT NULL,
    entity_id    VARCHAR(36),
    description  TEXT,
    before_value TEXT,
    after_value  TEXT,
    ip_address   VARCHAR(45),
    request_id   VARCHAR(36),
    occurred_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_entity    ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_user      ON audit_log(user_id);
CREATE INDEX idx_audit_timestamp ON audit_log(occurred_at DESC);

-- Immutability trigger
CREATE OR REPLACE FUNCTION fn_reject_modification()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Rows in % are immutable. Corrections must be new entries.', TG_TABLE_NAME;
END;
$$;

CREATE TRIGGER trg_audit_log_no_update
    BEFORE UPDATE ON audit_log
    FOR EACH ROW EXECUTE FUNCTION fn_reject_modification();

CREATE TRIGGER trg_audit_log_no_delete
    BEFORE DELETE ON audit_log
    FOR EACH ROW EXECUTE FUNCTION fn_reject_modification();
```

---

### ledger_entries — Fully Immutable

```sql
CREATE TRIGGER trg_ledger_no_update
    BEFORE UPDATE ON ledger_entries
    FOR EACH ROW EXECUTE FUNCTION fn_reject_modification();

CREATE TRIGGER trg_ledger_no_delete
    BEFORE DELETE ON ledger_entries
    FOR EACH ROW EXECUTE FUNCTION fn_reject_modification();
```

---

### library_custody_log — Partially Immutable

```sql
-- Core issue fields are immutable; return fields can be set once
CREATE OR REPLACE FUNCTION fn_custody_log_partial_immutable()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.copy_id           IS DISTINCT FROM NEW.copy_id          OR
       OLD.borrower_user_id  IS DISTINCT FROM NEW.borrower_user_id OR
       OLD.issued_at         IS DISTINCT FROM NEW.issued_at         OR
       OLD.issued_by_user_id IS DISTINCT FROM NEW.issued_by_user_id THEN
        RAISE EXCEPTION 'Core fields of library_custody_log are immutable';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_custody_partial_immutable
    BEFORE UPDATE ON library_custody_log
    FOR EACH ROW EXECUTE FUNCTION fn_custody_log_partial_immutable();

CREATE TRIGGER trg_custody_no_delete
    BEFORE DELETE ON library_custody_log
    FOR EACH ROW EXECUTE FUNCTION fn_reject_modification();
```

---

### asset_custody_log — Fully Immutable

```sql
CREATE TRIGGER trg_asset_custody_no_update
    BEFORE UPDATE ON asset_custody_log
    FOR EACH ROW EXECUTE FUNCTION fn_reject_modification();

CREATE TRIGGER trg_asset_custody_no_delete
    BEFORE DELETE ON asset_custody_log
    FOR EACH ROW EXECUTE FUNCTION fn_reject_modification();
```

---

## 18. Indexes Summary

| Table | Index | Columns | Type |
|---|---|---|---|
| users | idx_users_username | username | Unique partial (not deleted) |
| users | idx_users_email | email | Unique partial (not deleted) |
| role_assignments | idx_role_assignment_user | user_id | Regular |
| permission_grants | idx_pg_grantee | grantee_user_id | Regular |
| refresh_tokens | idx_refresh_token_hash | token_hash | Unique |
| departments | idx_dept_code | code | Unique partial |
| courses | idx_course_code | code | Unique partial |
| students | idx_student_enrollment | enrollment_number | Unique partial |
| enrollments | idx_enrollment_current | student_id, is_current | Regular |
| attendance_sessions | idx_att_session_offering | offering_id | Regular |
| attendance_records | idx_att_record_student | student_id | Regular |
| marks_entries | idx_marks_student | student_id | Regular |
| fee_invoices | idx_invoice_student | student_id | Regular |
| fee_payments | idx_payment_invoice | invoice_id | Regular |
| ledger_entries | idx_ledger_date | entry_date DESC | Regular |
| book_copies | idx_copy_barcode | barcode | Unique partial |
| library_custody_log | idx_custody_overdue | due_at | Partial (not returned) |
| library_fines | idx_fine_user | user_id | Regular |
| assets | idx_asset_tag | asset_tag | Unique partial |
| notification_records | idx_notification_unread | user_id, is_read | Partial (unread) |
| background_jobs | idx_bg_job_pending | status, scheduled_for | Partial (PENDING) |
| audit_log | idx_audit_timestamp | occurred_at DESC | Regular |

---

## 19. Seed Data (V014)

The V014 migration inserts the following baseline data required for the system to function:

### Account Heads

| Code | Name | Type |
|---|---|---|
| AR001 | Student Fee Receivable | ASSET |
| INC001 | Fee Income | INCOME |
| EXP001 | Staff Salaries | EXPENSE |
| EXP002 | Library Expenses | EXPENSE |
| EXP003 | Asset Purchases | ASSET |
| LIAB001 | Advance Fees | LIABILITY |

### Fee Heads

| Code | Name |
|---|---|
| TUITION | Tuition Fee |
| LAB | Laboratory Fee |
| LIBRARY | Library Fee |
| EXAM | Examination Fee |
| SPORTS | Sports Fee |
| HOSTEL | Hostel Fee |
| TRANSPORT | Transport Fee |
| MISC | Miscellaneous Fee |

### Leave Types

| Code | Name | Days/Year | Paid |
|---|---|---|---|
| CL | Casual Leave | 12 | Yes |
| SL | Sick Leave | 10 | Yes |
| EL | Earned Leave | 15 | Yes |
| ML | Maternity Leave | 26 | Yes |
| PL | Paternity Leave | 5 | Yes |
| LOP | Loss of Pay | 999 | No |

### Loan Policies

| Borrower Type | Max Books | Loan Days | Fine/Day | Max Fine |
|---|---|---|---|---|
| STUDENT | 3 | 14 | ₹2.00 | ₹200.00 |
| FACULTY | 10 | 30 | ₹0.00 | ₹0.00 |
| STAFF | 5 | 21 | ₹1.00 | ₹100.00 |

---

*Document Owner: Database Architecture Team*  
*Last Updated: June 2026*
