package com.college.erp.common.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

/**
 * {@link BaseEntity} plus a {@code tenant_id} column, for entities that belong to exactly one
 * college in this system's multi-tenant SaaS design (08-system-architecture.md §5.2, ADR — shared
 * schema, Postgres Row-Level Security). Deliberately a separate mapped superclass from
 * {@code BaseEntity} rather than adding {@code tenantId} there directly: every entity in every
 * module currently extends {@code BaseEntity}, and the tenancy retrofit is being rolled out
 * module-by-module (see CLAUDE.md's Build progress table), not system-wide in one pass. Adding the
 * column to {@code BaseEntity} itself would make Hibernate expect a {@code tenant_id} column on
 * every {@code BaseEntity}-backed table across all 16 modules immediately — breaking every module
 * whose own migration hasn't added that column yet.
 *
 * <p>Extend this class (not {@code BaseEntity}) once a module's own tenant-retrofit migration adds
 * {@code tenant_id} + Row-Level Security to its tables — see {@code V017__multi_tenancy_foundation
 * .sql} for the exact per-table pattern (column, backfill, FK, index, and either an RLS policy or a
 * documented reason the table is scoped in application code instead, as {@code module.auth}'s
 * {@code users}/{@code role_assignments} are). The two classes can be merged once every module has
 * been converted.
 *
 * <p>{@code tenantId} is populated from {@link com.college.erp.tenant.TenantContext} at the point
 * an entity is built — see any of {@code module.academic}/{@code module.student}'s
 * {@code *Service.create()} methods for the pattern. It is intentionally never accepted directly
 * from a client request DTO: which tenant a new row belongs to is always the *caller's own*
 * tenant, resolved server-side from their authenticated session, never a value the client supplies.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class TenantScopedEntity extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
}
