-- HR module: designations, staff_profiles, leave_types, leave_balances, leave_requests.
-- Source of truth: 04-database-schema.md §13.
--
-- 08-system-architecture.md's file tree also names AppraisalCycle/AppraisalForm domain classes and
-- an AppraisalService/AppraisalController, but none has any DDL anywhere in
-- 04-database-schema.md, nor any [HR-xxx] requirement in 03-srs.md §3.10, nor any endpoint in
-- 05-api-documentation.md §10 — same undocumented-entity situation as examination/finance/
-- library/asset. Deferred.

CREATE TABLE designations (
    id         UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    grade      VARCHAR(20),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);

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
