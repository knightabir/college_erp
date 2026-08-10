-- Fixes a live bug in V017's 13 tenant_isolation RLS policies: under connection pooling, once a
-- pooled connection has ever had `SET app.current_tenant = '<uuid>'` executed on it (TenantAware-
-- DataSource, on a tenant-scoped request), Postgres treats app.current_tenant as an established
-- placeholder GUC on that connection from then on. A later `RESET app.current_tenant` on the SAME
-- physical connection (e.g. the next checkout goes to an unauthenticated request, a scheduled job,
-- or any other tenant-less code path) does NOT make current_setting(..., true) return NULL as
-- TenantAwareDataSource's javadoc assumed — it resets the placeholder to '' (empty string)
-- instead. Every policy below then evaluates `'' ::UUID`, which Postgres cannot cast, raising
-- "invalid input syntax for type uuid" and failing the query outright (first observed via
-- AttendanceShortfallScanJob querying subject_offerings — a scheduled job has no tenant context
-- at all, and the pooled connection it happened to receive had previously served a tenant-scoped
-- request).
--
-- Fix: wrap current_setting(...) in NULLIF(..., '') before the ::UUID cast in every affected
-- policy, so both "never set" (NULL) and "reset after being set" ('') collapse to the same NULL
-- value before casting. `tenant_id = NULL` is still always unknown/false either way — the
-- fail-closed behavior V017 intended (a connection with no real tenant context sees zero rows on
-- every RLS-protected table) is unchanged, this only removes the cast error.
--
-- Same 13 tables V017 touched (module.academic's 9 + module.student's 4) — no other schema change.
-- TenantAwareDataSource's javadoc is updated in the same commit to describe RESET's actual
-- behavior accurately; the SET/RESET code itself doesn't need to change, since NULLIF here makes
-- either outcome equally safe to evaluate.

DROP POLICY tenant_isolation ON departments;
CREATE POLICY tenant_isolation ON departments
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::UUID)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::UUID);

DROP POLICY tenant_isolation ON academic_years;
CREATE POLICY tenant_isolation ON academic_years
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::UUID)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::UUID);

DROP POLICY tenant_isolation ON courses;
CREATE POLICY tenant_isolation ON courses
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::UUID)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::UUID);

DROP POLICY tenant_isolation ON terms;
CREATE POLICY tenant_isolation ON terms
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::UUID)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::UUID);

DROP POLICY tenant_isolation ON batches;
CREATE POLICY tenant_isolation ON batches
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::UUID)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::UUID);

DROP POLICY tenant_isolation ON sections;
CREATE POLICY tenant_isolation ON sections
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::UUID)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::UUID);

DROP POLICY tenant_isolation ON subjects;
CREATE POLICY tenant_isolation ON subjects
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::UUID)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::UUID);

DROP POLICY tenant_isolation ON subject_offerings;
CREATE POLICY tenant_isolation ON subject_offerings
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::UUID)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::UUID);

DROP POLICY tenant_isolation ON faculty_subject_mappings;
CREATE POLICY tenant_isolation ON faculty_subject_mappings
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::UUID)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::UUID);

DROP POLICY tenant_isolation ON students;
CREATE POLICY tenant_isolation ON students
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::UUID)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::UUID);

DROP POLICY tenant_isolation ON enrollments;
CREATE POLICY tenant_isolation ON enrollments
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::UUID)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::UUID);

DROP POLICY tenant_isolation ON guardians;
CREATE POLICY tenant_isolation ON guardians
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::UUID)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::UUID);

DROP POLICY tenant_isolation ON student_documents;
CREATE POLICY tenant_isolation ON student_documents
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::UUID)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::UUID);
