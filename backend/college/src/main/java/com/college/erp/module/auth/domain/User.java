package com.college.erp.module.auth.domain;

import java.time.Instant;

import com.college.erp.common.domain.TenantScopedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 04-database-schema.md §5.1. username/email uniqueness among non-deleted rows is enforced by the
 * partial unique indexes in V001 — not repeated here as a JPA @Column(unique=true), since that
 * would apply globally including soft-deleted rows. Deliberately stays GLOBALLY unique even after
 * V017's multi-tenancy retrofit (not scoped per-tenant like department/course codes) — see V017's
 * header comment: login resolves a user by username/email alone, with no tenant selector on the
 * login form, so identifiers must stay unambiguous across the whole platform.
 *
 * Plain @Builder per 07-coding-standards.md §2.2 (not @SuperBuilder) — it only covers this
 * class's own fields, not TenantScopedEntity/BaseEntity's. That's intentional: id/timestamps are
 * populated by JPA (@GeneratedValue, auditing listeners) at persist time, never through the
 * builder — tenantId is the one TenantScopedEntity field callers DO set through the builder
 * (UserManagementService/BootstrapAdminRunner/TenantRegistrationService all do).
 */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "users",
    indexes = {
        @Index(name = "idx_users_username", columnList = "username"),
        @Index(name = "idx_users_email", columnList = "email")
    }
)
public class User extends TenantScopedEntity {

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false, columnDefinition = "TEXT")
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(name = "is_locked", nullable = false)
    @Builder.Default
    private boolean locked = false;

    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private int failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "totp_secret", columnDefinition = "TEXT")
    private String totpSecret;

    @Column(name = "totp_enabled", nullable = false)
    @Builder.Default
    private boolean totpEnabled = false;

    @Column(name = "must_change_password", nullable = false)
    @Builder.Default
    private boolean mustChangePassword = false;
}
