package com.college.erp.security.rbac;

import java.util.List;
import java.util.UUID;

/**
 * Dependency-inversion seam: the security kernel defines what it needs, module.auth provides it
 * (a @Component implementing this, backed by PermissionGrantRepository). This keeps the module
 * dependency graph a DAG — kernel never imports a feature module's repository directly
 * (08-system-architecture.md §3.1) — while still letting GrantResolver read grant data.
 */
public interface PermissionGrantSource {

    List<ActiveGrant> findActiveGrants(UUID userId);
}
