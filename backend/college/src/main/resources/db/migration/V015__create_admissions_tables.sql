-- Admissions module: applications, application_documents, application_workflow_steps
-- (append-only), merit_lists, merit_list_entries, seat_allotments, lateral_entry_applications.
--
-- UNLIKE every other migration in this project, there is NO source-of-truth DDL for this in
-- 04-database-schema.md — the schema doc has no "Module: Admissions" section at all (it jumps
-- from §16 Notifications/Scheduler straight to §17 Append-Only Tables). 03-srs.md and
-- 05-api-documentation.md are likewise silent. Only 08-system-architecture.md's file tree names
-- the domain classes (Application, ApplicationDocument, ApplicationWorkflowStep, MeritList,
-- SeatAllotment, LateralEntryApplication) with no field lists. This schema is therefore
-- INVENTED — built by this session at the user's explicit request to complete the backend before
-- moving to the frontend, following this project's established conventions (UUID PKs, soft
-- delete, TIMESTAMPTZ, the append-only-workflow-log pattern already used for
-- asset_custody_log/library_custody_log) as the closest available guide. Treat every table below
-- as a best-effort placeholder, not an authoritative spec — replace with real DDL the moment
-- 04-database-schema.md/03-srs.md/05-api-documentation.md gain an actual Admissions section.
--
-- V014 is deliberately skipped — 04-database-schema.md §19 reserves "V014" for seed data, which
-- doesn't exist as a migration yet; starting this invented schema at V015 avoids colliding with
-- that number if seed data is added later.

CREATE TABLE applications (
    id                      UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    application_number      VARCHAR(30)   NOT NULL UNIQUE,
    applicant_name          VARCHAR(200)  NOT NULL,
    applicant_email         VARCHAR(150)  NOT NULL,
    applicant_phone         VARCHAR(20),
    date_of_birth           DATE,
    gender                  VARCHAR(10),
    course_id               UUID          NOT NULL REFERENCES courses(id),
    academic_year_id        UUID          NOT NULL REFERENCES academic_years(id),
    category                VARCHAR(20),
    -- GENERAL | OBC | SC | ST | EWS — mirrors module.student's StudentCategory
    qualifying_exam_score   NUMERIC(5,2),
    qualifying_exam_details TEXT,
    status                  VARCHAR(20)   NOT NULL DEFAULT 'SUBMITTED',
    -- SUBMITTED | UNDER_REVIEW | SHORTLISTED | REJECTED | MERIT_LISTED | SEAT_ALLOTTED | ADMITTED | WITHDRAWN
    submitted_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);

CREATE INDEX idx_application_course ON applications(course_id, academic_year_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_application_status ON applications(status) WHERE is_deleted = FALSE;

CREATE TABLE application_documents (
    id                  UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    application_id      UUID         NOT NULL REFERENCES applications(id),
    document_type       VARCHAR(50)  NOT NULL,
    file_id             VARCHAR(36),
    verification_status VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    -- PENDING | VERIFIED | REJECTED
    verified_by         UUID         REFERENCES users(id),
    verified_at         TIMESTAMPTZ,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);

CREATE INDEX idx_app_doc_application ON application_documents(application_id) WHERE is_deleted = FALSE;

-- Append-only status-transition log — same pattern as asset_custody_log/library_custody_log
-- (04-database-schema.md §17), reused here since applications.status changes need an audit trail
-- distinct from the generic audit_log (this one is domain-queryable: "show me this application's
-- full review history").
CREATE TABLE application_workflow_steps (
    id            UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    application_id UUID       NOT NULL REFERENCES applications(id),
    from_status   VARCHAR(20),
    to_status     VARCHAR(20) NOT NULL,
    remarks       TEXT,
    actioned_by   UUID        NOT NULL REFERENCES users(id),
    actioned_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
    -- NO is_deleted/updated_at — fully immutable via trigger, matching §17's pattern.
);

CREATE INDEX idx_workflow_step_application ON application_workflow_steps(application_id);

CREATE TRIGGER trg_app_workflow_step_no_update
    BEFORE UPDATE ON application_workflow_steps
    FOR EACH ROW EXECUTE FUNCTION fn_reject_modification();

CREATE TRIGGER trg_app_workflow_step_no_delete
    BEFORE DELETE ON application_workflow_steps
    FOR EACH ROW EXECUTE FUNCTION fn_reject_modification();

CREATE TABLE merit_lists (
    id                UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    course_id         UUID         NOT NULL REFERENCES courses(id),
    academic_year_id  UUID         NOT NULL REFERENCES academic_years(id),
    name              VARCHAR(200) NOT NULL,
    published_at      TIMESTAMPTZ,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);

CREATE TABLE merit_list_entries (
    id             UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    merit_list_id  UUID          NOT NULL REFERENCES merit_lists(id),
    application_id UUID          NOT NULL REFERENCES applications(id),
    rank           INT           NOT NULL,
    score          NUMERIC(5,2),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID,
    UNIQUE (merit_list_id, application_id),
    UNIQUE (merit_list_id, rank)
);

CREATE TABLE seat_allotments (
    id                 UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    application_id     UUID         NOT NULL REFERENCES applications(id),
    merit_list_id      UUID         REFERENCES merit_lists(id),
    course_id          UUID         NOT NULL REFERENCES courses(id),
    batch_id           UUID         REFERENCES batches(id),
    status             VARCHAR(20)  NOT NULL DEFAULT 'OFFERED',
    -- OFFERED | ACCEPTED | DECLINED | EXPIRED
    offered_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    response_deadline  TIMESTAMPTZ,
    responded_at       TIMESTAMPTZ,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);

CREATE INDEX idx_seat_allotment_application ON seat_allotments(application_id) WHERE is_deleted = FALSE;

CREATE TABLE lateral_entry_applications (
    id                  UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    application_id      UUID         NOT NULL UNIQUE REFERENCES applications(id),
    diploma_institution VARCHAR(200),
    diploma_score       NUMERIC(5,2),
    entry_year_number   INT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);
