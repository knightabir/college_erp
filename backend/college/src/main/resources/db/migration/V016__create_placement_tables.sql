-- Placement module: companies, placement_drives, drive_applications, interview_rounds,
-- placement_offers.
--
-- Same situation as V015 (admissions) — NO source-of-truth DDL anywhere in
-- 04-database-schema.md/03-srs.md/05-api-documentation.md, only a file-tree class listing in
-- 08-system-architecture.md (Company, PlacementDrive, DriveApplication, InterviewRound,
-- PlacementOffer). This schema is INVENTED, built at the user's explicit request to complete the
-- backend before moving to the frontend — see V015's header comment for the full reasoning, which
-- applies identically here. Treat as a best-effort placeholder, not an authoritative spec.

CREATE TABLE companies (
    id             UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name           VARCHAR(200) NOT NULL,
    industry       VARCHAR(100),
    website        VARCHAR(200),
    contact_person VARCHAR(100),
    contact_email  VARCHAR(150),
    contact_phone  VARCHAR(20),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);

CREATE TABLE placement_drives (
    id                  UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    company_id          UUID          NOT NULL REFERENCES companies(id),
    academic_year_id    UUID          NOT NULL REFERENCES academic_years(id),
    title               VARCHAR(200)  NOT NULL,
    description         TEXT,
    eligible_course_id  UUID          REFERENCES courses(id),
    -- NULL = open to every course
    eligibility_min_cgpa NUMERIC(4,2),
    ctc_offered         NUMERIC(12,2),
    drive_date          DATE,
    status              VARCHAR(20)   NOT NULL DEFAULT 'SCHEDULED',
    -- SCHEDULED | ONGOING | COMPLETED | CANCELLED
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);

CREATE INDEX idx_drive_company ON placement_drives(company_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_drive_status  ON placement_drives(status)     WHERE is_deleted = FALSE;

CREATE TABLE drive_applications (
    id          UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    drive_id    UUID        NOT NULL REFERENCES placement_drives(id),
    student_id  UUID        NOT NULL REFERENCES students(id),
    status      VARCHAR(20) NOT NULL DEFAULT 'APPLIED',
    -- APPLIED | SHORTLISTED | INTERVIEWING | SELECTED | REJECTED | WITHDRAWN
    applied_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID,
    UNIQUE (drive_id, student_id)
);

CREATE INDEX idx_drive_app_student ON drive_applications(student_id) WHERE is_deleted = FALSE;

CREATE TABLE interview_rounds (
    id                   UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    drive_application_id UUID         NOT NULL REFERENCES drive_applications(id),
    round_number         INT          NOT NULL,
    round_type           VARCHAR(30),
    -- APTITUDE | TECHNICAL | HR | GROUP_DISCUSSION | OTHER
    scheduled_at         TIMESTAMPTZ,
    result               VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    -- PENDING | CLEARED | REJECTED
    feedback             TEXT,
    interviewer_name     VARCHAR(100),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID,
    UNIQUE (drive_application_id, round_number)
);

CREATE TABLE placement_offers (
    id                   UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    drive_application_id UUID          NOT NULL UNIQUE REFERENCES drive_applications(id),
    ctc                  NUMERIC(12,2) NOT NULL,
    designation          VARCHAR(100),
    offer_date           DATE          NOT NULL,
    joining_date         DATE,
    status               VARCHAR(20)   NOT NULL DEFAULT 'OFFERED',
    -- OFFERED | ACCEPTED | DECLINED | WITHDRAWN
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);
