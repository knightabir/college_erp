package com.college.erp.tenant.domain;

import com.college.erp.common.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A single college using this platform — the "tenant" in this system's shared-schema, Row-Level
 * Security multi-tenancy design (08-system-architecture.md §5.2). Deliberately lives in the
 * platform kernel ({@code com.college.erp.tenant}, not {@code module.tenant}) — every module will
 * eventually depend on "which tenant is this row in", the same reasoning that already places
 * {@code audit}/{@code notification}/{@code storage}/{@code security} in the kernel rather than
 * inside one specific feature module.
 *
 * <p>Extends {@code BaseEntity} directly, not {@link com.college.erp.common.domain.TenantScopedEntity}
 * — a Tenant row IS a tenant, it doesn't belong to one.
 *
 * <p>V017__multi_tenancy_foundation.sql's own header comment covers the full schema design
 * (isolation strategy, why {@code users}/{@code role_assignments} are scoped in application code
 * instead of RLS, the {@code erp_app} least-privilege runtime role). This class's field list is
 * this session's best-effort "college details" — no planning document specifies one (multi-tenancy
 * was named only as a one-paragraph future-cloud-mode aspiration in 08-system-architecture.md §5.2,
 * with zero DDL/FRs/endpoints anywhere else) — see RegisterCollegeRequest's javadoc for the same
 * disclosure on the registration payload shape.
 */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tenants", indexes = @Index(name = "idx_tenant_code", columnList = "code"))
public class Tenant extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "code", nullable = false, length = 30)
    private String code;

    @Column(name = "affiliated_university", length = 200)
    private String affiliatedUniversity;

    @Column(name = "accreditation", length = 100)
    private String accreditation;

    @Column(name = "established_year")
    private Integer establishedYear;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "pincode", length = 10)
    private String pincode;

    @Builder.Default
    @Column(name = "country", nullable = false, length = 100)
    private String country = "India";

    @Column(name = "contact_email", nullable = false, length = 150)
    private String contactEmail;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "website", length = 200)
    private String website;

    /** Handle returned by FileStorageService.store() — not an FK, same convention as
     *  StudentDocument.fileId (module.storage isn't wired up to any upload call site yet). */
    @Column(name = "logo_file_id", length = 36)
    private String logoFileId;

    @Builder.Default
    @Column(name = "timezone", nullable = false, length = 50)
    private String timezone = "Asia/Kolkata";

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TenantStatus status = TenantStatus.ACTIVE;
}
