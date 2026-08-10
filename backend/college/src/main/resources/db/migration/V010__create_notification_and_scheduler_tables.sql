-- Notification + scheduler kernel: notification_templates, notification_records,
-- background_jobs. Source of truth: 04-database-schema.md §16.
--
-- Both notification and scheduler are platform-kernel packages (com.college.erp.notification,
-- com.college.erp.scheduler), not module/ packages — 06-design-document.md's dependency diagram
-- (§6) places them alongside auth/audit/storage as things every module depends on, not the other
-- way around. This DDL has no deviations from §16 — it's copied through as documented.

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
