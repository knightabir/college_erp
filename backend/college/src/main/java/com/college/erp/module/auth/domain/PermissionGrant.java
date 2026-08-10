package com.college.erp.module.auth.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 04-database-schema.md §5.3. Scoped, revocable grants — Layer 2 of permission evaluation
 * (08-system-architecture.md §4.2). FR-RBAC-3: a user must never be able to grant themselves a
 * permission — enforced in PermissionGrantService, not here.
 */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "permission_grants",
    indexes = @Index(name = "idx_pg_grantee", columnList = "grantee_user_id")
)
public class PermissionGrant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "grantee_user_id", nullable = false)
    private UUID granteeUserId;

    @Column(name = "granted_by_user_id", nullable = false)
    private UUID grantedByUserId;

    @Column(name = "permission_key", nullable = false, length = 100)
    private String permissionKey;

    // No FK — departments/batches tables do not exist until V003.
    @Column(name = "scope_department_id")
    private UUID scopeDepartmentId;

    @Column(name = "scope_batch_id")
    private UUID scopeBatchId;

    @Column(name = "scope_fee_head", length = 100)
    private String scopeFeeHead;

    @Column(name = "amount_threshold", precision = 14, scale = 2)
    private BigDecimal amountThreshold;

    @Builder.Default
    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt = Instant.now();

    @Builder.Default
    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom = Instant.now();

    /** NULL = permanent until revoked. */
    @Column(name = "effective_until")
    private Instant effectiveUntil;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoked_by_user_id")
    private UUID revokedByUserId;

    public boolean isCurrentlyActive() {
        Instant now = Instant.now();
        return revokedAt == null
                && !effectiveFrom.isAfter(now)
                && (effectiveUntil == null || effectiveUntil.isAfter(now));
    }
}
