-- Examination module: exams, hall_tickets, marks_entries, result_records, revaluation_requests.
-- Source of truth: 04-database-schema.md §9.
--
-- 08-system-architecture.md's file tree also names ExamSchedule/SeatingAllocation/
-- InvigilatorAllocation/QuestionPaper domain classes, but none of the four have any DDL anywhere
-- in 04-database-schema.md (unlike student_documents, which at least got a passing mention this
-- has none) — deferred entirely rather than inventing four more undocumented schemas on top of
-- the five tables that ARE specified. See CLAUDE.md.
--
-- exams.status has no value list in the doc's comment (unlike exam_type, which does) — inferred
-- SCHEDULED | ONGOING | COMPLETED | CANCELLED, enforced only in the Java enum, not a CHECK
-- constraint, consistent with how every other status column in this schema is handled.
-- revaluation_requests.status is the same situation — inferred PENDING | COMPLETED | REJECTED
-- from the columns that exist (reviewed_marks/reviewed_by/reviewed_at, no separate "assigned"
-- tracking column).

CREATE TABLE exams (
    id          UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    exam_type   VARCHAR(30)  NOT NULL,
    batch_id    UUID         NOT NULL REFERENCES batches(id),
    term_id     UUID         NOT NULL REFERENCES terms(id),
    start_date  DATE         NOT NULL,
    end_date    DATE         NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'SCHEDULED',
    is_deleted  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by  UUID,
    updated_by  UUID
);

CREATE TABLE hall_tickets (
    id                    UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    exam_id               UUID        NOT NULL REFERENCES exams(id),
    student_id            UUID        NOT NULL REFERENCES students(id),
    hall_ticket_number    VARCHAR(30) NOT NULL UNIQUE,
    is_eligible           BOOLEAN     NOT NULL DEFAULT TRUE,
    ineligibility_reason  TEXT,
    issued_at             TIMESTAMPTZ,
    is_deleted            BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by            UUID,
    updated_by            UUID,
    UNIQUE (exam_id, student_id)
);

CREATE INDEX idx_hall_ticket_exam    ON hall_tickets(exam_id);
CREATE INDEX idx_hall_ticket_student ON hall_tickets(student_id);

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
    entered_by      UUID          REFERENCES users(id),
    moderated_by    UUID          REFERENCES users(id),
    moderated_at    TIMESTAMPTZ,
    is_deleted      BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by      UUID,
    updated_by      UUID,
    UNIQUE (exam_id, offering_id, student_id)
);

CREATE INDEX idx_marks_student ON marks_entries(student_id);
CREATE INDEX idx_marks_exam    ON marks_entries(exam_id);

CREATE TABLE result_records (
    id                    UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    student_id            UUID          NOT NULL REFERENCES students(id),
    batch_id              UUID          NOT NULL REFERENCES batches(id),
    term_id               UUID          NOT NULL REFERENCES terms(id),
    sgpa                  NUMERIC(4,2),
    cgpa                  NUMERIC(4,2),
    total_credits_earned  INT,
    result_status         VARCHAR(20)   NOT NULL,
    published_at          TIMESTAMPTZ,
    is_deleted            BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by            UUID,
    updated_by            UUID,
    UNIQUE (student_id, term_id)
);

CREATE TABLE revaluation_requests (
    id             UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    marks_entry_id UUID        NOT NULL REFERENCES marks_entries(id),
    student_id     UUID        NOT NULL REFERENCES students(id),
    reason         TEXT,
    fee_payment_id UUID,
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewed_marks NUMERIC(6,2),
    reviewed_by    UUID        REFERENCES users(id),
    reviewed_at    TIMESTAMPTZ,
    is_deleted     BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by     UUID,
    updated_by     UUID
);
