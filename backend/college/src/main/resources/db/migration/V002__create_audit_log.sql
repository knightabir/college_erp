-- Platform kernel: audit_log, append-only enforced by trigger.
-- fn_reject_modification() is shared — reused by V007 (ledger_entries), V008
-- (library_custody_log), V009 (asset_custody_log). Defined once here.
-- Source of truth: 04-database-schema.md §17.

CREATE TABLE audit_log (
    id           UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id      UUID,
    username     VARCHAR(100),
    action       VARCHAR(50)  NOT NULL,
    module       VARCHAR(50),
    entity_type  VARCHAR(100) NOT NULL,
    entity_id    VARCHAR(36),
    description  TEXT,
    before_value TEXT,
    after_value  TEXT,
    ip_address   VARCHAR(45),
    request_id   VARCHAR(36),
    occurred_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_entity    ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_user      ON audit_log(user_id);
CREATE INDEX idx_audit_timestamp ON audit_log(occurred_at DESC);

-- Shared immutability trigger function — reused by every append-only table in this system.
CREATE OR REPLACE FUNCTION fn_reject_modification()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Rows in % are immutable. Corrections must be new entries.', TG_TABLE_NAME;
END;
$$;

CREATE TRIGGER trg_audit_log_no_update
    BEFORE UPDATE ON audit_log
    FOR EACH ROW EXECUTE FUNCTION fn_reject_modification();

CREATE TRIGGER trg_audit_log_no_delete
    BEFORE DELETE ON audit_log
    FOR EACH ROW EXECUTE FUNCTION fn_reject_modification();
