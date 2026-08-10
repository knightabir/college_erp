package com.college.erp.module.auth.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 04-database-schema.md §5.2. Does not extend BaseEntity — this table has its own shape
 * (assigned_at/expires_at/is_active, no soft-delete/updated_at/updated_by).
 */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "role_assignments",
    indexes = {
        @Index(name = "idx_role_assignment_user", columnList = "user_id"),
        @Index(name = "idx_role_assignment_role", columnList = "role")
    },
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "role", "department_id"})
)
public class RoleAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Added by V017 — see UserManagementService's class javadoc: tracked here for
     *  application-level tenant scoping, deliberately not Postgres-RLS-protected (same reasoning
     *  as User.tenantId). */
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private UserRole role;

    // No FK — departments table does not exist until V003 (see V001 migration comment).
    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "assigned_by", nullable = false)
    private UUID assignedBy;

    @Builder.Default
    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt = Instant.now();

    /** NULL = permanent. */
    @Column(name = "expires_at")
    private Instant expiresAt;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public boolean isCurrentlyEffective() {
        return active && (expiresAt == null || expiresAt.isAfter(Instant.now()));
    }
}
