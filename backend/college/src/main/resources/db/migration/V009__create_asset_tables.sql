-- Asset module: asset_categories, assets, asset_custody_log (fully immutable), maintenance_tickets.
-- Source of truth: 04-database-schema.md §12.
--
-- 08-system-architecture.md's file tree also names AssetTag, MaintenanceSchedule,
-- AnnualMaintenanceContract, AssetDisposal, DepreciationRecord, ConsumableItem and RoomBooking
-- domain/service classes, but none has any DDL anywhere in 04-database-schema.md — same
-- undocumented-entity situation as examination/finance/library. Deferred. asset_tag is just a
-- column on assets (no separate table needed), and per 03-srs.md [AST-004] disposal is a plain
-- status transition on assets.status rather than a separate disposal record.
--
-- Two FK omissions fixed here, matching the pattern already applied to
-- attendance_policies.course_id in V005: assets.department_id and
-- asset_custody_log.to_department_id are both NOT NULL in 04-database-schema.md but lack a
-- REFERENCES clause there even though departments(id) exists (V003).

CREATE TABLE asset_categories (
    id                    UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name                  VARCHAR(100) NOT NULL,
    depreciation_rate_pct INT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);

CREATE TABLE assets (
    id                    UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    asset_tag             VARCHAR(50)   NOT NULL UNIQUE,
    name                  VARCHAR(200)  NOT NULL,
    description           TEXT,
    category_id           UUID          REFERENCES asset_categories(id),
    department_id         UUID          NOT NULL REFERENCES departments(id),
    room_location         VARCHAR(100),
    purchase_date         DATE,
    purchase_cost         NUMERIC(14,2),
    vendor_id             UUID          REFERENCES vendors(id),
    warranty_expiry       DATE,
    assigned_to_user_id   UUID          REFERENCES users(id),
    status                VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    -- ACTIVE | UNDER_MAINTENANCE | DISPOSED | LOST | TRANSFERRED
    barcode_image_file_id VARCHAR(36),
    photo_file_id         VARCHAR(36),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);

CREATE UNIQUE INDEX idx_asset_tag  ON assets(asset_tag)     WHERE is_deleted = FALSE;
CREATE INDEX idx_asset_dept        ON assets(department_id) WHERE is_deleted = FALSE;

-- Fully immutable (append-only) — no is_deleted/updated_at, matching ledger_entries/audit_log.
CREATE TABLE asset_custody_log (
    id                     UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    asset_id               UUID        NOT NULL REFERENCES assets(id),
    from_department_id     UUID        REFERENCES departments(id),
    to_department_id       UUID        NOT NULL REFERENCES departments(id),
    from_user_id           UUID        REFERENCES users(id),
    assigned_to_user_id    UUID        REFERENCES users(id),
    transferred_by_user_id UUID        NOT NULL REFERENCES users(id),
    transferred_at         TIMESTAMPTZ NOT NULL,
    reason                 TEXT,
    from_location           VARCHAR(100),
    to_location             VARCHAR(100),
    created_at             TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_acl_asset    ON asset_custody_log(asset_id);
CREATE INDEX idx_acl_assignee ON asset_custody_log(assigned_to_user_id);

CREATE TRIGGER trg_acl_no_update
    BEFORE UPDATE ON asset_custody_log
    FOR EACH ROW EXECUTE FUNCTION fn_reject_modification();

CREATE TRIGGER trg_acl_no_delete
    BEFORE DELETE ON asset_custody_log
    FOR EACH ROW EXECUTE FUNCTION fn_reject_modification();

CREATE TABLE maintenance_tickets (
    id           UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    asset_id     UUID         NOT NULL REFERENCES assets(id),
    reported_by  UUID         NOT NULL REFERENCES users(id),
    title        VARCHAR(200) NOT NULL,
    description  TEXT,
    priority     VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',
    -- LOW | MEDIUM | HIGH | CRITICAL
    status       VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    -- OPEN | ASSIGNED | IN_PROGRESS | RESOLVED | CLOSED | CANCELLED
    assigned_to  UUID         REFERENCES users(id),
    resolved_at  TIMESTAMPTZ,
    resolution   TEXT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);

CREATE INDEX idx_mt_asset  ON maintenance_tickets(asset_id);
CREATE INDEX idx_mt_status ON maintenance_tickets(status);
