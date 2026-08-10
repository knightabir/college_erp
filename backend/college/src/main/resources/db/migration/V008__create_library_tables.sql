-- Library module: loan_policies, book_titles, book_copies, library_custody_log (partially
-- immutable), reservations, library_fines. Source of truth: 04-database-schema.md §11.
--
-- 08-system-architecture.md's file tree also names LibraryMember/EResource/LibraryAcquisition
-- domain classes and a StockVerificationService, but none has any DDL anywhere in
-- 04-database-schema.md — same undocumented-entity situation as examination/finance. Deferred.
-- Borrowers don't need a library_members table anyway: library_custody_log.borrower_user_id
-- already references users(id) directly, scoped by borrower_type against loan_policies.

CREATE TABLE loan_policies (
    id               UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    borrower_type    VARCHAR(30)   NOT NULL UNIQUE,
    max_books        INT           NOT NULL DEFAULT 3,
    loan_days        INT           NOT NULL DEFAULT 14,
    renewals_allowed INT           NOT NULL DEFAULT 2,
    fine_per_day     NUMERIC(8,2)  NOT NULL DEFAULT 2.00,
    max_fine         NUMERIC(10,2) DEFAULT 200.00,
    is_deleted       BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by       UUID,
    updated_by       UUID
);

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
    is_deleted             BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by             UUID,
    updated_by             UUID
);

CREATE INDEX idx_book_isbn    ON book_titles(isbn)                  WHERE is_deleted = FALSE;
CREATE INDEX idx_book_subject ON book_titles(subject_classification) WHERE is_deleted = FALSE;

CREATE TABLE book_copies (
    id                    UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    title_id              UUID        NOT NULL REFERENCES book_titles(id),
    barcode               VARCHAR(50) NOT NULL UNIQUE,
    accession_number      VARCHAR(30) NOT NULL UNIQUE,
    shelf_location        VARCHAR(50),
    rack_number           VARCHAR(20),
    condition             VARCHAR(20) NOT NULL DEFAULT 'GOOD',
    status                VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    barcode_image_file_id VARCHAR(36),
    is_deleted            BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by            UUID,
    updated_by            UUID
);

CREATE UNIQUE INDEX idx_copy_barcode ON book_copies(barcode)  WHERE is_deleted = FALSE;
CREATE INDEX idx_copy_title          ON book_copies(title_id) WHERE is_deleted = FALSE;

-- Append-only, partially immutable — see 04-database-schema.md §17 and V002's shared
-- fn_reject_modification(). Core issue fields locked; return fields settable once.
CREATE TABLE library_custody_log (
    id                   UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    copy_id              UUID          NOT NULL REFERENCES book_copies(id),
    borrower_user_id     UUID          NOT NULL REFERENCES users(id),
    borrower_name        VARCHAR(200)  NOT NULL,
    borrower_type        VARCHAR(20)   NOT NULL,
    issued_by_user_id    UUID          NOT NULL REFERENCES users(id),
    returned_by_user_id  UUID          REFERENCES users(id),
    issued_at            TIMESTAMPTZ   NOT NULL,
    due_at               TIMESTAMPTZ   NOT NULL,
    returned_at          TIMESTAMPTZ,
    condition_on_issue   VARCHAR(20),
    condition_on_return  VARCHAR(20),
    fine_amount          NUMERIC(10,2),
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_custody_copy     ON library_custody_log(copy_id);
CREATE INDEX idx_custody_borrower ON library_custody_log(borrower_user_id);
CREATE INDEX idx_custody_overdue  ON library_custody_log(due_at) WHERE returned_at IS NULL;

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

CREATE TABLE reservations (
    id             UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    title_id       UUID        NOT NULL REFERENCES book_titles(id),
    user_id        UUID        NOT NULL REFERENCES users(id),
    queue_position INT         NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    notified_at    TIMESTAMPTZ,
    expires_at     TIMESTAMPTZ,
    is_deleted     BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by     UUID,
    updated_by     UUID
);

CREATE INDEX idx_reservation_title ON reservations(title_id);

CREATE TABLE library_fines (
    id                UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id           UUID          NOT NULL REFERENCES users(id),
    custody_log_id    UUID          NOT NULL REFERENCES library_custody_log(id),
    amount            NUMERIC(10,2) NOT NULL,
    paid_amount       NUMERIC(10,2) NOT NULL DEFAULT 0,
    status            VARCHAR(20)   NOT NULL DEFAULT 'UNPAID',
    waived_by_user_id UUID          REFERENCES users(id),
    waiver_reason     TEXT,
    is_deleted        BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by        UUID,
    updated_by        UUID
);

CREATE INDEX idx_fine_user ON library_fines(user_id);
