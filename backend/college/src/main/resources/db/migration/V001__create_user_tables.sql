-- Auth & Security module: users, role_assignments, permission_grants, refresh_tokens.
-- Source of truth: 04-database-schema.md §5.

-- =====================================================================
-- users
-- =====================================================================
CREATE TABLE users (
    id                    UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    username              VARCHAR(50)  NOT NULL UNIQUE,
    email                 VARCHAR(150) NOT NULL UNIQUE,
    password_hash         TEXT         NOT NULL,
    full_name             VARCHAR(150) NOT NULL,
    phone                 VARCHAR(20),
    is_enabled            BOOLEAN      NOT NULL DEFAULT TRUE,
    is_locked             BOOLEAN      NOT NULL DEFAULT FALSE,
    failed_login_attempts INT          NOT NULL DEFAULT 0,
    locked_until          TIMESTAMPTZ,
    last_login_at         TIMESTAMPTZ,
    totp_secret           TEXT,                          -- encrypted TOTP seed
    totp_enabled          BOOLEAN      NOT NULL DEFAULT FALSE,
    must_change_password  BOOLEAN      NOT NULL DEFAULT FALSE,
    is_deleted            BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by            UUID,
    updated_by            UUID
);

-- Partial unique indexes so a soft-deleted user's username/email can be reissued.
CREATE UNIQUE INDEX idx_users_username ON users(username) WHERE is_deleted = FALSE;
CREATE UNIQUE INDEX idx_users_email    ON users(email)    WHERE is_deleted = FALSE;

-- =====================================================================
-- role_assignments
-- =====================================================================
CREATE TABLE role_assignments (
    id            UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id       UUID         NOT NULL REFERENCES users(id),
    role          VARCHAR(50)  NOT NULL,       -- enum: SUPER_ADMIN, ADMIN, HOD, FACULTY, etc.
    department_id UUID,                         -- optional scope: limits HOD to one department
                                                  -- no FK yet — departments table arrives in V003
    assigned_by   UUID         NOT NULL,       -- who created this assignment
    assigned_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expires_at    TIMESTAMPTZ,                  -- NULL = permanent
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    UNIQUE (user_id, role, department_id)
);

CREATE INDEX idx_role_assignment_user ON role_assignments(user_id);
CREATE INDEX idx_role_assignment_role ON role_assignments(role);

-- =====================================================================
-- permission_grants
-- =====================================================================
CREATE TABLE permission_grants (
    id                  UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    grantee_user_id     UUID          NOT NULL REFERENCES users(id),
    granted_by_user_id  UUID          NOT NULL REFERENCES users(id),
    permission_key      VARCHAR(100)  NOT NULL,   -- e.g. 'FINANCE.FEE_COLLECTION'
    scope_department_id UUID,                      -- no FK yet — departments table arrives in V003
    scope_batch_id      UUID,                      -- no FK yet — batches table arrives in V003
    scope_fee_head      VARCHAR(100),
    amount_threshold    NUMERIC(14,2),
    granted_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    effective_from      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    effective_until     TIMESTAMPTZ,              -- NULL = permanent until revoked
    revoked_at          TIMESTAMPTZ,
    revoked_by_user_id  UUID
);

CREATE INDEX idx_pg_grantee ON permission_grants(grantee_user_id);

-- =====================================================================
-- refresh_tokens
-- =====================================================================
CREATE TABLE refresh_tokens (
    id           UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id      UUID         NOT NULL REFERENCES users(id),
    token_hash   VARCHAR(64)  NOT NULL UNIQUE,  -- SHA-256 hex of raw token, never the raw token
    device_info  VARCHAR(255),
    ip_address   VARCHAR(45),
    expires_at   TIMESTAMPTZ  NOT NULL,
    revoked_at   TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_token_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_token_user ON refresh_tokens(user_id);
