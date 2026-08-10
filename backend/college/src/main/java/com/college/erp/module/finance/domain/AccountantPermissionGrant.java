package com.college.erp.module.finance.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.college.erp.common.domain.BaseEntity;

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
 * 04-database-schema.md §10.8, 03-srs.md [FIN-008]. Unlike module.auth's PermissionGrant (§5.3,
 * which has no is_deleted/updated_at/updated_by), this table has the full base column set, so it
 * extends BaseEntity. Feeds the kernel's GrantResolver via AccountantPermissionGrantSourceImpl —
 * see GrantResolver javadoc for why both grant tables are aggregated rather than one replacing
 * the other.
 */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "accountant_permission_grants", indexes = @Index(name = "idx_apg_grantee", columnList = "grantee_user_id"))
public class AccountantPermissionGrant extends BaseEntity {

    @Column(name = "grantee_user_id", nullable = false)
    private UUID granteeUserId;

    @Column(name = "granted_by_user_id", nullable = false)
    private UUID grantedByUserId;

    @Column(name = "permission_key", nullable = false, length = 100)
    private String permissionKey;

    @Column(name = "scope_department_id")
    private UUID scopeDepartmentId;

    @Column(name = "scope_batch_id")
    private UUID scopeBatchId;

    @Column(name = "scope_fee_head_code", length = 30)
    private String scopeFeeHeadCode;

    @Column(name = "amount_threshold", precision = 14, scale = 2)
    private BigDecimal amountThreshold;

    @Builder.Default
    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom = Instant.now();

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
