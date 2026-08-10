-- Academic module: departments, academic_years, courses, terms, batches, sections, subjects,
-- subject_offerings, faculty_subject_mappings. Source of truth: 04-database-schema.md §6.
-- Backfills the FKs V001 deliberately left off (role_assignments.department_id,
-- permission_grants.scope_department_id/scope_batch_id) now that departments/batches exist.

CREATE TABLE departments (
    id             UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name           VARCHAR(150) NOT NULL,
    code           VARCHAR(20)  NOT NULL,
    description    TEXT,
    hod_user_id    UUID REFERENCES users(id),
    contact_email  VARCHAR(150),
    contact_phone  VARCHAR(20),
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    is_deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by     UUID,
    updated_by     UUID
);

CREATE UNIQUE INDEX idx_dept_code ON departments(code) WHERE is_deleted = FALSE;

CREATE TABLE academic_years (
    id         UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    label      VARCHAR(20) NOT NULL UNIQUE,
    start_year INT         NOT NULL,
    end_year   INT         NOT NULL,
    is_current BOOLEAN     NOT NULL DEFAULT FALSE,
    is_deleted BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID
);

-- Only one row may have is_current = TRUE.
CREATE UNIQUE INDEX idx_acyr_current ON academic_years(is_current) WHERE is_current = TRUE;

CREATE TABLE courses (
    id              UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    department_id   UUID         NOT NULL REFERENCES departments(id),
    name            VARCHAR(200) NOT NULL,
    code            VARCHAR(20)  NOT NULL,
    level           VARCHAR(20)  NOT NULL,
    duration_years  INT          NOT NULL,
    total_credits   INT          NOT NULL DEFAULT 0,
    intake_capacity INT          NOT NULL,
    description     TEXT,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    is_deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by      UUID,
    updated_by      UUID
);

CREATE UNIQUE INDEX idx_course_code ON courses(code) WHERE is_deleted = FALSE;
CREATE INDEX idx_course_dept ON courses(department_id);

CREATE TABLE terms (
    id               UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    academic_year_id UUID        NOT NULL REFERENCES academic_years(id),
    name             VARCHAR(50) NOT NULL,
    term_number      INT         NOT NULL,
    start_date       DATE        NOT NULL,
    end_date         DATE        NOT NULL,
    exam_start_date  DATE,
    exam_end_date    DATE,
    is_active        BOOLEAN     NOT NULL DEFAULT FALSE,
    is_deleted       BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by       UUID,
    updated_by       UUID
);

CREATE INDEX idx_term_academic_year ON terms(academic_year_id);

CREATE TABLE batches (
    id                   UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    course_id            UUID         NOT NULL REFERENCES courses(id),
    academic_year_id     UUID         NOT NULL REFERENCES academic_years(id),
    label                VARCHAR(100) NOT NULL,
    intake_year          INT          NOT NULL,
    current_term_number  INT          NOT NULL DEFAULT 1,
    is_active            BOOLEAN      NOT NULL DEFAULT TRUE,
    is_deleted           BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by           UUID,
    updated_by           UUID,
    UNIQUE (course_id, academic_year_id)
);

CREATE INDEX idx_batch_course ON batches(course_id);

CREATE TABLE sections (
    id         UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    batch_id   UUID        NOT NULL REFERENCES batches(id),
    name       VARCHAR(10) NOT NULL,
    capacity   INT         NOT NULL,
    is_active  BOOLEAN     NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    UNIQUE (batch_id, name)
);

CREATE TABLE subjects (
    id            UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    code          VARCHAR(20)  NOT NULL,
    name          VARCHAR(200) NOT NULL,
    department_id UUID         NOT NULL REFERENCES departments(id),
    credits       INT          NOT NULL DEFAULT 0,
    type          VARCHAR(20)  NOT NULL,
    description   TEXT,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    is_deleted    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by    UUID,
    updated_by    UUID
);

CREATE UNIQUE INDEX idx_subject_code ON subjects(code) WHERE is_deleted = FALSE;

CREATE TABLE subject_offerings (
    id                   UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    batch_id             UUID        NOT NULL REFERENCES batches(id),
    subject_id           UUID        NOT NULL REFERENCES subjects(id),
    term_id              UUID        NOT NULL REFERENCES terms(id),
    max_internal_marks   INT         NOT NULL DEFAULT 40,
    max_external_marks   INT         NOT NULL DEFAULT 60,
    pass_marks_internal  INT         NOT NULL DEFAULT 16,
    pass_marks_external  INT         NOT NULL DEFAULT 24,
    is_elective          BOOLEAN     NOT NULL DEFAULT FALSE,
    is_deleted           BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by           UUID,
    updated_by           UUID,
    UNIQUE (batch_id, subject_id, term_id)
);

CREATE TABLE faculty_subject_mappings (
    id              UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    offering_id     UUID        NOT NULL REFERENCES subject_offerings(id),
    section_id      UUID        NOT NULL REFERENCES sections(id),
    faculty_user_id UUID        NOT NULL REFERENCES users(id),
    is_primary      BOOLEAN     NOT NULL DEFAULT TRUE,
    is_active       BOOLEAN     NOT NULL DEFAULT TRUE,
    is_deleted      BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      UUID,
    updated_by      UUID,
    UNIQUE (offering_id, section_id, faculty_user_id)
);

-- Backfill the FKs deliberately left off in V001 (departments/batches didn't exist yet then).
ALTER TABLE role_assignments
    ADD CONSTRAINT fk_role_assignment_department FOREIGN KEY (department_id) REFERENCES departments(id);

ALTER TABLE permission_grants
    ADD CONSTRAINT fk_permission_grant_department FOREIGN KEY (scope_department_id) REFERENCES departments(id),
    ADD CONSTRAINT fk_permission_grant_batch FOREIGN KEY (scope_batch_id) REFERENCES batches(id);
