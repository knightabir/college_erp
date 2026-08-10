package com.college.erp.security.rbac;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * The dimensions a scoped permission_grant can be narrowed by (04-database-schema.md §5.3).
 * Pass null for any dimension not relevant to the check being performed.
 */
public record PermissionScope(UUID departmentId, UUID batchId, String feeHead, BigDecimal amount) {

    public static PermissionScope none() {
        return new PermissionScope(null, null, null, null);
    }

    public static PermissionScope ofDepartment(UUID departmentId) {
        return new PermissionScope(departmentId, null, null, null);
    }
}
