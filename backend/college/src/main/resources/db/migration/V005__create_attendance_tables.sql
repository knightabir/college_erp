-- Attendance module: attendance_policies, attendance_sessions, attendance_records,
-- condonation_requests. Source of truth: 04-database-schema.md §8.
--
-- attendance_policies.course_id is UNIQUE but has no REFERENCES in the doc's own DDL (every other
-- course_id column in the schema does have one) — adding the FK here since courses already exists
-- (V003) and every sign points to this being a documentation omission, not intentional.

CREATE TABLE attendance_policies (
    id                        UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    course_id                 UUID        NOT NULL UNIQUE REFERENCES courses(id),
    min_attendance_pct        INT         NOT NULL DEFAULT 75,
    medical_condonation_pct   INT         NOT NULL DEFAULT 65,
    consecutive_absence_alert INT         NOT NULL DEFAULT 3,
    is_deleted                BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at                TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by                UUID,
    updated_by                UUID
);

CREATE TABLE attendance_sessions (
    id              UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    offering_id     UUID        NOT NULL REFERENCES subject_offerings(id),
    section_id      UUID        NOT NULL REFERENCES sections(id),
    faculty_user_id UUID        NOT NULL REFERENCES users(id),
    session_date    DATE        NOT NULL,
    start_time      TIME,
    end_time        TIME,
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    remarks         TEXT,
    is_deleted      BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      UUID,
    updated_by      UUID,
    UNIQUE (offering_id, section_id, session_date)
);

CREATE INDEX idx_att_session_offering ON attendance_sessions(offering_id);
CREATE INDEX idx_att_session_section  ON attendance_sessions(section_id);
CREATE INDEX idx_att_session_date     ON attendance_sessions(session_date);

CREATE TABLE attendance_records (
    id         UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    session_id UUID        NOT NULL REFERENCES attendance_sessions(id),
    student_id UUID        NOT NULL REFERENCES students(id),
    status     VARCHAR(20) NOT NULL,
    remarks    VARCHAR(255),
    is_deleted BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    UNIQUE (session_id, student_id)
);

CREATE INDEX idx_att_record_session ON attendance_records(session_id);
CREATE INDEX idx_att_record_student ON attendance_records(student_id);

CREATE TABLE condonation_requests (
    id                  UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    student_id          UUID        NOT NULL REFERENCES students(id),
    offering_id         UUID        NOT NULL REFERENCES subject_offerings(id),
    term_id             UUID        NOT NULL REFERENCES terms(id),
    reason              TEXT        NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewed_by_user_id UUID        REFERENCES users(id),
    review_remarks      TEXT,
    is_deleted          BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by          UUID,
    updated_by          UUID
);

CREATE INDEX idx_cond_student ON condonation_requests(student_id);
