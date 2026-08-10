package com.college.erp.security.rbac;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Minimal projection of an active, non-expired, non-revoked permission_grant row — just the
 * fields Layer 2 of permission evaluation needs. Kept separate from the module.auth
 * PermissionGrant entity so the security kernel never imports a feature module's domain type.
 */
public record ActiveGrant(
        String permissionKey,
        UUID scopeDepartmentId,
        UUID scopeBatchId,
        String scopeFeeHead,
        BigDecimal amountThreshold) {

    public boolean coversScope(PermissionScope requested) {
        if (requested == null) {
            return true;
        }
        if (scopeDepartmentId != null && !scopeDepartmentId.equals(requested.departmentId())) {
            return false;
        }
        if (scopeBatchId != null && !scopeBatchId.equals(requested.batchId())) {
            return false;
        }
        if (scopeFeeHead != null && !scopeFeeHead.equals(requested.feeHead())) {
            return false;
        }
        return amountThreshold == null
                || requested.amount() == null
                || requested.amount().compareTo(amountThreshold) <= 0;
    }
}
