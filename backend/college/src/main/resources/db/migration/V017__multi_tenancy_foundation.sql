-- Multi-tenancy foundation (08-system-architecture.md §5.2 "Multi-Tenancy (Cloud Mode)").
--
-- This system is now a SaaS platform: any number of colleges ("tenants") can share one deployment,
-- each seeing only their own data. Isolation strategy is "shared schema + tenant_id + Postgres RLS"
-- (a college registers via POST /api/v1/register, which creates a `tenants` row plus that
-- college's own SUPER_ADMIN — see tenant.controller.RegistrationController).
--
-- Scope of this migration — matches CLAUDE.md's "foundation + reference modules" note: the
-- `tenants` table itself, plus tenant_id retrofitted onto every table in the three reference
-- modules (auth, academic, student). All 13 other modules are NOT touched here — their tables have
-- no tenant_id yet and are not RLS-protected. Converting them is follow-up work, module by module,
-- following the exact pattern below.
--
-- Two different enforcement layers for two different reasons:
--   1. `users`/`role_assignments` (module.auth) get a tenant_id COLUMN but NO RLS policy. Login
--      resolves a user by username/email ALONE (no tenant selector on the login form — see
--      AuthService.login()/UserRepository.findByUsernameOrEmail) and username/email stay globally
--      unique across the whole platform (unlike department/course codes below) specifically so
--      that lookup stays unambiguous without one. RLS keyed on tenant_id would make that lookup
--      return zero rows before the tenant is even known, breaking login outright. Tenant scoping
--      for these two tables is enforced in application code instead (UserManagementService reads
--      TenantContext.getCurrentTenantId() explicitly) — see CLAUDE.md's module/auth deviations.
--   2. `departments`/`courses`/.../`students`/`guardians`/... (module.academic + module.student)
--      get tenant_id AND real Postgres RLS — there is no legitimate cross-tenant read case for any
--      of this data, so the database itself refuses to return or accept a row for the wrong
--      tenant, independent of whether every code path remembered to filter correctly.
--
-- RLS is enforced correctly ONLY when the connecting DB role is not a superuser and does not own
-- the tables (Postgres unconditionally exempts superusers/owners from their own RLS policies, no
-- override exists). The role Flyway just used to run this migration (see application.yaml's
-- spring.flyway.* vs spring.datasource.* split) is a superuser locally — so this migration also
-- creates a dedicated least-privilege `erp_app` role for the application's own runtime connection
-- (spring.datasource.*), which is what actually makes the RLS policies below take effect. See
-- TenantAwareDataSource's javadoc for how `app.current_tenant` gets set per request.

-- =====================================================================
-- tenants — the "college details" every registration collects
-- =====================================================================
CREATE TABLE tenants (
    id                     UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name                   VARCHAR(200) NOT NULL,
    code                   VARCHAR(30)  NOT NULL,          -- short slug, e.g. "srcem" — used nowhere
                                                             -- security-sensitive yet (no subdomain
                                                             -- routing), just a human-friendly handle
    affiliated_university  VARCHAR(200),
    accreditation          VARCHAR(100),                    -- e.g. "NAAC A+", free text
    established_year       INT,
    address                TEXT,
    city                   VARCHAR(100),
    state                  VARCHAR(100),
    pincode                VARCHAR(10),
    country                VARCHAR(100) NOT NULL DEFAULT 'India',
    contact_email          VARCHAR(150) NOT NULL,
    contact_phone          VARCHAR(20),
    website                VARCHAR(200),
    logo_file_id           VARCHAR(36),                     -- module.storage handle, not an FK
    timezone               VARCHAR(50)  NOT NULL DEFAULT 'Asia/Kolkata',
    status                 VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE | SUSPENDED
    is_deleted             BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by             UUID,
    updated_by             UUID
);

CREATE UNIQUE INDEX idx_tenant_code ON tenants(code) WHERE is_deleted = FALSE;

-- =====================================================================
-- One bootstrap tenant for existing data + air-gap/on-prem single-college deployments.
-- Matches 08-system-architecture.md §5.2: "Single-tenant (on-prem/air-gap) deployments use the
-- same schema with a single fixed tenant row — no code changes between modes." BootstrapAdminRunner
-- creates this same row idempotently on every fresh deployment too (see its javadoc) — inserted
-- here as well so THIS migration is self-contained against a database that already has rows to
-- backfill (e.g. this project's own dev database, which already has one bootstrap SUPER_ADMIN).
-- =====================================================================
INSERT INTO tenants (id, name, code, contact_email, status)
SELECT gen_random_uuid(), 'Default College', 'default', 'admin@college.edu', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM tenants WHERE code = 'default');

-- =====================================================================
-- users / role_assignments (module.auth) — column only, no RLS (see header note above)
-- =====================================================================
ALTER TABLE users ADD COLUMN tenant_id UUID;
UPDATE users SET tenant_id = (SELECT id FROM tenants WHERE code = 'default') WHERE tenant_id IS NULL;
ALTER TABLE users ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE users ADD CONSTRAINT fk_users_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
CREATE INDEX idx_users_tenant ON users(tenant_id);

ALTER TABLE role_assignments ADD COLUMN tenant_id UUID;
UPDATE role_assignments SET tenant_id = (SELECT id FROM tenants WHERE code = 'default') WHERE tenant_id IS NULL;
ALTER TABLE role_assignments ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE role_assignments ADD CONSTRAINT fk_role_assignment_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
CREATE INDEX idx_role_assignment_tenant ON role_assignments(tenant_id);

-- =====================================================================
-- module.academic — column + RLS on all 9 tables
-- =====================================================================

-- departments
ALTER TABLE departments ADD COLUMN tenant_id UUID;
UPDATE departments SET tenant_id = (SELECT id FROM tenants WHERE code = 'default') WHERE tenant_id IS NULL;
ALTER TABLE departments ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE departments ADD CONSTRAINT fk_departments_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
CREATE INDEX idx_departments_tenant ON departments(tenant_id);
DROP INDEX idx_dept_code;
CREATE UNIQUE INDEX idx_dept_code ON departments(tenant_id, code) WHERE is_deleted = FALSE;
ALTER TABLE departments ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON departments
    USING (tenant_id = current_setting('app.current_tenant', true)::UUID)
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true)::UUID);

-- academic_years
ALTER TABLE academic_years ADD COLUMN tenant_id UUID;
UPDATE academic_years SET tenant_id = (SELECT id FROM tenants WHERE code = 'default') WHERE tenant_id IS NULL;
ALTER TABLE academic_years ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE academic_years ADD CONSTRAINT fk_academic_years_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
CREATE INDEX idx_academic_years_tenant ON academic_years(tenant_id);
ALTER TABLE academic_years DROP CONSTRAINT academic_years_label_key;
CREATE UNIQUE INDEX idx_acyr_label ON academic_years(tenant_id, label) WHERE is_deleted = FALSE;
-- is_current's old global partial-unique index becomes per-tenant: two colleges may each have
-- their own "current" academic year at the same time.
DROP INDEX idx_acyr_current;
CREATE UNIQUE INDEX idx_acyr_current ON academic_years(tenant_id) WHERE is_current = TRUE;
ALTER TABLE academic_years ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON academic_years
    USING (tenant_id = current_setting('app.current_tenant', true)::UUID)
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true)::UUID);

-- courses
ALTER TABLE courses ADD COLUMN tenant_id UUID;
UPDATE courses SET tenant_id = (SELECT id FROM tenants WHERE code = 'default') WHERE tenant_id IS NULL;
ALTER TABLE courses ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE courses ADD CONSTRAINT fk_courses_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
CREATE INDEX idx_courses_tenant ON courses(tenant_id);
DROP INDEX idx_course_code;
CREATE UNIQUE INDEX idx_course_code ON courses(tenant_id, code) WHERE is_deleted = FALSE;
ALTER TABLE courses ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON courses
    USING (tenant_id = current_setting('app.current_tenant', true)::UUID)
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true)::UUID);

-- terms
ALTER TABLE terms ADD COLUMN tenant_id UUID;
UPDATE terms SET tenant_id = (SELECT id FROM tenants WHERE code = 'default') WHERE tenant_id IS NULL;
ALTER TABLE terms ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE terms ADD CONSTRAINT fk_terms_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
CREATE INDEX idx_terms_tenant ON terms(tenant_id);
ALTER TABLE terms ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON terms
    USING (tenant_id = current_setting('app.current_tenant', true)::UUID)
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true)::UUID);

-- batches (course_id/academic_year_id are already tenant-anchored via FK, so its existing
-- UNIQUE(course_id, academic_year_id) needs no change — a course_id can only ever belong to one
-- tenant)
ALTER TABLE batches ADD COLUMN tenant_id UUID;
UPDATE batches SET tenant_id = (SELECT id FROM tenants WHERE code = 'default') WHERE tenant_id IS NULL;
ALTER TABLE batches ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE batches ADD CONSTRAINT fk_batches_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
CREATE INDEX idx_batches_tenant ON batches(tenant_id);
ALTER TABLE batches ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON batches
    USING (tenant_id = current_setting('app.current_tenant', true)::UUID)
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true)::UUID);

-- sections (batch_id is already tenant-anchored)
ALTER TABLE sections ADD COLUMN tenant_id UUID;
UPDATE sections SET tenant_id = (SELECT id FROM tenants WHERE code = 'default') WHERE tenant_id IS NULL;
ALTER TABLE sections ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE sections ADD CONSTRAINT fk_sections_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
CREATE INDEX idx_sections_tenant ON sections(tenant_id);
ALTER TABLE sections ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON sections
    USING (tenant_id = current_setting('app.current_tenant', true)::UUID)
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true)::UUID);

-- subjects
ALTER TABLE subjects ADD COLUMN tenant_id UUID;
UPDATE subjects SET tenant_id = (SELECT id FROM tenants WHERE code = 'default') WHERE tenant_id IS NULL;
ALTER TABLE subjects ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE subjects ADD CONSTRAINT fk_subjects_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
CREATE INDEX idx_subjects_tenant ON subjects(tenant_id);
DROP INDEX idx_subject_code;
CREATE UNIQUE INDEX idx_subject_code ON subjects(tenant_id, code) WHERE is_deleted = FALSE;
ALTER TABLE subjects ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON subjects
    USING (tenant_id = current_setting('app.current_tenant', true)::UUID)
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true)::UUID);

-- subject_offerings (batch_id/subject_id/term_id already tenant-anchored)
ALTER TABLE subject_offerings ADD COLUMN tenant_id UUID;
UPDATE subject_offerings SET tenant_id = (SELECT id FROM tenants WHERE code = 'default') WHERE tenant_id IS NULL;
ALTER TABLE subject_offerings ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE subject_offerings ADD CONSTRAINT fk_subject_offerings_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
CREATE INDEX idx_subject_offerings_tenant ON subject_offerings(tenant_id);
ALTER TABLE subject_offerings ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON subject_offerings
    USING (tenant_id = current_setting('app.current_tenant', true)::UUID)
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true)::UUID);

-- faculty_subject_mappings (offering_id/section_id already tenant-anchored)
ALTER TABLE faculty_subject_mappings ADD COLUMN tenant_id UUID;
UPDATE faculty_subject_mappings SET tenant_id = (SELECT id FROM tenants WHERE code = 'default') WHERE tenant_id IS NULL;
ALTER TABLE faculty_subject_mappings ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE faculty_subject_mappings ADD CONSTRAINT fk_faculty_subject_mappings_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
CREATE INDEX idx_faculty_subject_mappings_tenant ON faculty_subject_mappings(tenant_id);
ALTER TABLE faculty_subject_mappings ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON faculty_subject_mappings
    USING (tenant_id = current_setting('app.current_tenant', true)::UUID)
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true)::UUID);

-- =====================================================================
-- module.student — column + RLS on all 4 tables
-- =====================================================================

-- students
ALTER TABLE students ADD COLUMN tenant_id UUID;
UPDATE students SET tenant_id = (SELECT id FROM tenants WHERE code = 'default') WHERE tenant_id IS NULL;
ALTER TABLE students ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE students ADD CONSTRAINT fk_students_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
CREATE INDEX idx_students_tenant ON students(tenant_id);
DROP INDEX idx_student_enrollment;
CREATE UNIQUE INDEX idx_student_enrollment ON students(tenant_id, enrollment_number) WHERE is_deleted = FALSE;
ALTER TABLE students ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON students
    USING (tenant_id = current_setting('app.current_tenant', true)::UUID)
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true)::UUID);

-- enrollments (student_id/batch_id/section_id/term_id already tenant-anchored)
ALTER TABLE enrollments ADD COLUMN tenant_id UUID;
UPDATE enrollments SET tenant_id = (SELECT id FROM tenants WHERE code = 'default') WHERE tenant_id IS NULL;
ALTER TABLE enrollments ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE enrollments ADD CONSTRAINT fk_enrollments_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
CREATE INDEX idx_enrollments_tenant ON enrollments(tenant_id);
ALTER TABLE enrollments ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON enrollments
    USING (tenant_id = current_setting('app.current_tenant', true)::UUID)
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true)::UUID);

-- guardians (student_id already tenant-anchored)
ALTER TABLE guardians ADD COLUMN tenant_id UUID;
UPDATE guardians SET tenant_id = (SELECT id FROM tenants WHERE code = 'default') WHERE tenant_id IS NULL;
ALTER TABLE guardians ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE guardians ADD CONSTRAINT fk_guardians_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
CREATE INDEX idx_guardians_tenant ON guardians(tenant_id);
ALTER TABLE guardians ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON guardians
    USING (tenant_id = current_setting('app.current_tenant', true)::UUID)
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true)::UUID);

-- student_documents (student_id already tenant-anchored)
ALTER TABLE student_documents ADD COLUMN tenant_id UUID;
UPDATE student_documents SET tenant_id = (SELECT id FROM tenants WHERE code = 'default') WHERE tenant_id IS NULL;
ALTER TABLE student_documents ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE student_documents ADD CONSTRAINT fk_student_documents_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
CREATE INDEX idx_student_documents_tenant ON student_documents(tenant_id);
ALTER TABLE student_documents ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON student_documents
    USING (tenant_id = current_setting('app.current_tenant', true)::UUID)
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true)::UUID);

-- =====================================================================
-- Least-privilege runtime role — see header note. Table/sequence grants use ALTER DEFAULT
-- PRIVILEGES too, so tables created by future migrations (any module, not just these three) are
-- automatically usable by the app without a further GRANT each time.
-- Password matches ErpProperties/application.yaml's spring.datasource.password default — override
-- both together via DB_PASSWORD for any non-local deployment, same as every other credential
-- default in this project (erp.bootstrap.admin.password, etc.).
-- =====================================================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'erp_app') THEN
        CREATE ROLE erp_app LOGIN PASSWORD 'ErpApp@2026';
    END IF;
END
$$;

GRANT CONNECT ON DATABASE college_erp TO erp_app;
GRANT USAGE ON SCHEMA public TO erp_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO erp_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO erp_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO erp_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO erp_app;
