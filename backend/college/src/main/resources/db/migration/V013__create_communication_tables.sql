-- Communication module: notices, helpdesk_tickets, ticket_comments.
-- Source of truth: 04-database-schema.md §15, 03-srs.md §3.11 [COMM-001]/[COMM-002].
--
-- [COMM-002] says helpdesk tickets are "assigned to the relevant department", but
-- helpdesk_tickets has no department_id column anywhere in this DDL — only assigned_to (a direct
-- user reference). Followed the schema as written: assignment is to a specific staff user, not a
-- department queue. See the module's CLAUDE.md deviations note.

CREATE TABLE notices (
    id           UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    title        VARCHAR(300) NOT NULL,
    body         TEXT         NOT NULL,
    target_type  VARCHAR(20)  NOT NULL DEFAULT 'ALL',
    -- ALL | DEPARTMENT | BATCH | ROLE
    target_id    UUID,                       -- department_id or batch_id when targeted
    is_pinned    BOOLEAN      NOT NULL DEFAULT FALSE,
    published_at TIMESTAMPTZ,
    expires_at   TIMESTAMPTZ,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);

CREATE TABLE helpdesk_tickets (
    id                UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    raised_by_user_id UUID         NOT NULL REFERENCES users(id),
    title             VARCHAR(300) NOT NULL,
    description       TEXT         NOT NULL,
    category          VARCHAR(50)  NOT NULL,  -- ACADEMIC | FINANCIAL | IT | FACILITIES
    priority          VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',
    status            VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    assigned_to       UUID         REFERENCES users(id),
    resolved_at       TIMESTAMPTZ,
    resolution        TEXT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);

CREATE INDEX idx_ticket_user   ON helpdesk_tickets(raised_by_user_id);
CREATE INDEX idx_ticket_status ON helpdesk_tickets(status);

CREATE TABLE ticket_comments (
    id         UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    ticket_id  UUID        NOT NULL REFERENCES helpdesk_tickets(id),
    user_id    UUID        NOT NULL REFERENCES users(id),
    comment    TEXT        NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);
