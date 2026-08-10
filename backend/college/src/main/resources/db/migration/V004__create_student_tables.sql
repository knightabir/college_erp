-- Student module: students, enrollments (append-only by design — new row per section/batch/term
-- change, existing rows' is_current flipped rather than the row being edited), guardians,
-- student_documents. Source of truth: 04-database-schema.md §7 for the first three tables.
--
-- student_documents has NO DDL in 04-database-schema.md (§7 only covers 7.1-7.3) even though
-- 08-system-architecture.md's file tree and its own §12 migration-list comment both name it under
-- V004. Schema below is inferred from the file-storage design (08-system-architecture.md §8 —
-- fileId is a string handle, not an FK) and the DocumentVerificationService the file tree lists.

CREATE TABLE students (
    id                UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id           UUID         UNIQUE REFERENCES users(id),
    enrollment_number VARCHAR(30)  NOT NULL,
    first_name        VARCHAR(100) NOT NULL,
    last_name         VARCHAR(100) NOT NULL,
    personal_email    VARCHAR(150) NOT NULL,
    phone             VARCHAR(20),
    date_of_birth     DATE         NOT NULL,
    gender            VARCHAR(20)  NOT NULL,
    blood_group       VARCHAR(5),
    aadhar_number     VARCHAR(12),
    category          VARCHAR(30),
    nationality       VARCHAR(50)  NOT NULL DEFAULT 'Indian',
    address           TEXT,
    city              VARCHAR(100),
    state             VARCHAR(100),
    pincode           VARCHAR(10),
    photo_file_id     VARCHAR(36),
    status            VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    admission_date    DATE         NOT NULL,
    graduation_date   DATE,
    is_deleted        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by        UUID,
    updated_by        UUID
);

CREATE UNIQUE INDEX idx_student_enrollment ON students(enrollment_number) WHERE is_deleted = FALSE;
CREATE INDEX idx_student_email ON students(personal_email) WHERE is_deleted = FALSE;

CREATE TABLE enrollments (
    id              UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    student_id      UUID        NOT NULL REFERENCES students(id),
    batch_id        UUID        NOT NULL REFERENCES batches(id),
    section_id      UUID        NOT NULL REFERENCES sections(id),
    term_id         UUID        NOT NULL REFERENCES terms(id),
    enrollment_date DATE        NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    is_current      BOOLEAN     NOT NULL DEFAULT TRUE,
    remarks         TEXT,
    is_deleted      BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      UUID,
    updated_by      UUID
);

CREATE INDEX idx_enrollment_student ON enrollments(student_id);
CREATE INDEX idx_enrollment_section ON enrollments(section_id);
CREATE INDEX idx_enrollment_current ON enrollments(student_id, is_current);

CREATE TABLE guardians (
    id            UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    student_id    UUID         NOT NULL REFERENCES students(id),
    name          VARCHAR(150) NOT NULL,
    relationship  VARCHAR(30)  NOT NULL,
    phone         VARCHAR(20)  NOT NULL,
    email         VARCHAR(150),
    occupation    VARCHAR(100),
    annual_income BIGINT,
    is_primary    BOOLEAN      NOT NULL DEFAULT FALSE,
    user_id       UUID         REFERENCES users(id),
    is_deleted    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by    UUID,
    updated_by    UUID
);

CREATE INDEX idx_guardian_student ON guardians(student_id);

-- Inferred (see file header) — not in 04-database-schema.md.
CREATE TABLE student_documents (
    id                   UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    student_id           UUID         NOT NULL REFERENCES students(id),
    document_type        VARCHAR(50)  NOT NULL,
    file_id              VARCHAR(36)  NOT NULL,
    verification_status  VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    verified_by          UUID,
    verified_at          TIMESTAMPTZ,
    remarks              TEXT,
    is_deleted           BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by           UUID,
    updated_by           UUID
);

CREATE INDEX idx_student_document_student ON student_documents(student_id);
