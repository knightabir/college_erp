-- Finance module: financial_years, account_heads, fee_heads, fee_structures, fee_invoices,
-- fee_payments, ledger_entries (append-only), accountant_permission_grants, vendors,
-- purchase_orders. Source of truth: 04-database-schema.md §10.
--
-- 08-system-architecture.md's file tree also names Scholarship/Refund/Budget/Payroll/
-- BankReconciliation/FinancialReport controllers+services, but NONE of the underlying tables
-- (scholarships, refund_requests, budgets, payslips, ...) have any DDL anywhere in
-- 04-database-schema.md — same situation as examination's four undocumented entities. Deferred
-- entirely rather than inventing five more undocumented schemas. See CLAUDE.md.

CREATE TABLE financial_years (
    id                  UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    label               VARCHAR(20) NOT NULL UNIQUE,
    start_date          DATE        NOT NULL,
    end_date            DATE        NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    closed_by_user_id   UUID        REFERENCES users(id),
    approved_by_user_id UUID        REFERENCES users(id),
    is_deleted          BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by          UUID,
    updated_by          UUID
);

CREATE TABLE account_heads (
    id          UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    code        VARCHAR(30)  NOT NULL UNIQUE,
    name        VARCHAR(150) NOT NULL,
    description TEXT,
    type        VARCHAR(20)  NOT NULL,
    parent_id   UUID         REFERENCES account_heads(id),
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    is_deleted  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by  UUID,
    updated_by  UUID
);

CREATE TABLE fee_heads (
    id          UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    code        VARCHAR(30)  NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    is_deleted  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by  UUID,
    updated_by  UUID
);

CREATE TABLE fee_structures (
    id                   UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    course_id            UUID          NOT NULL REFERENCES courses(id),
    batch_id             UUID          REFERENCES batches(id),
    fee_head_id          UUID          NOT NULL REFERENCES fee_heads(id),
    term_number          INT           NOT NULL,
    amount               NUMERIC(14,2) NOT NULL,
    due_date_offset_days INT,
    late_fee_per_day     NUMERIC(10,2),
    is_active            BOOLEAN       NOT NULL DEFAULT TRUE,
    is_deleted           BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by           UUID,
    updated_by           UUID,
    UNIQUE (course_id, batch_id, fee_head_id, term_number)
);

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
    is_deleted       BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by       UUID,
    updated_by       UUID,
    UNIQUE (student_id, batch_id, term_number)
);

CREATE INDEX idx_invoice_student ON fee_invoices(student_id);
CREATE UNIQUE INDEX idx_invoice_number ON fee_invoices(invoice_number);

CREATE TABLE fee_payments (
    id                    UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    invoice_id            UUID          NOT NULL REFERENCES fee_invoices(id),
    receipt_number        VARCHAR(30)   NOT NULL UNIQUE,
    amount                NUMERIC(14,2) NOT NULL,
    payment_mode          VARCHAR(30)   NOT NULL,
    transaction_reference VARCHAR(100),
    payment_gateway       VARCHAR(50),
    collected_by_user_id  UUID          REFERENCES users(id),
    payment_timestamp     TIMESTAMPTZ   NOT NULL,
    status                VARCHAR(20)   NOT NULL DEFAULT 'SUCCESS',
    notes                 TEXT,
    is_deleted            BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by            UUID,
    updated_by            UUID
);

CREATE INDEX idx_payment_invoice ON fee_payments(invoice_id);

-- Append-only — see 04-database-schema.md §17 and V002's shared fn_reject_modification().
CREATE TABLE ledger_entries (
    id                UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    account_head_id   UUID          NOT NULL REFERENCES account_heads(id),
    description       TEXT          NOT NULL,
    entry_type        VARCHAR(10)   NOT NULL,
    amount            NUMERIC(14,2) NOT NULL,
    entry_date        DATE          NOT NULL,
    financial_year_id UUID          NOT NULL REFERENCES financial_years(id),
    reference_type    VARCHAR(50),
    reference_id      VARCHAR(36),
    department_id     UUID,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by        UUID
);

CREATE INDEX idx_ledger_account ON ledger_entries(account_head_id);
CREATE INDEX idx_ledger_date    ON ledger_entries(entry_date DESC);
CREATE INDEX idx_ledger_year    ON ledger_entries(financial_year_id);

CREATE TRIGGER trg_ledger_no_update
    BEFORE UPDATE ON ledger_entries
    FOR EACH ROW EXECUTE FUNCTION fn_reject_modification();

CREATE TRIGGER trg_ledger_no_delete
    BEFORE DELETE ON ledger_entries
    FOR EACH ROW EXECUTE FUNCTION fn_reject_modification();

CREATE TABLE accountant_permission_grants (
    id                   UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    grantee_user_id      UUID          NOT NULL REFERENCES users(id),
    granted_by_user_id   UUID          NOT NULL REFERENCES users(id),
    permission_key       VARCHAR(100)  NOT NULL,
    scope_department_id  UUID,
    scope_batch_id       UUID,
    scope_fee_head_code  VARCHAR(30),
    amount_threshold     NUMERIC(14,2),
    effective_from       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    effective_until      TIMESTAMPTZ,
    revoked_at           TIMESTAMPTZ,
    revoked_by_user_id   UUID          REFERENCES users(id),
    is_deleted           BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by           UUID,
    updated_by           UUID
);

CREATE INDEX idx_apg_grantee ON accountant_permission_grants(grantee_user_id);

CREATE TABLE vendors (
    id                  UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    code                VARCHAR(20)  NOT NULL UNIQUE,
    name                VARCHAR(200) NOT NULL,
    contact_person      VARCHAR(100),
    email               VARCHAR(150),
    phone               VARCHAR(20),
    address             TEXT,
    gst_number          VARCHAR(20),
    pan_number          VARCHAR(10),
    bank_account_number VARCHAR(30),
    ifsc_code           VARCHAR(11),
    is_active           BOOLEAN      NOT NULL DEFAULT TRUE,
    is_deleted          BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by          UUID,
    updated_by          UUID
);

CREATE TABLE purchase_orders (
    id                     UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    po_number              VARCHAR(30)   NOT NULL UNIQUE,
    vendor_id              UUID          NOT NULL REFERENCES vendors(id),
    department_id          UUID          REFERENCES departments(id),
    financial_year_id      UUID          NOT NULL REFERENCES financial_years(id),
    total_amount           NUMERIC(14,2) NOT NULL,
    description            TEXT,
    po_date                DATE          NOT NULL,
    expected_delivery_date DATE,
    status                 VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    approved_by_user_id    UUID          REFERENCES users(id),
    is_deleted             BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at             TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by             UUID,
    updated_by             UUID
);
