package com.college.erp.security.rbac;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

/**
 * Layer 1 of permission evaluation (08-system-architecture.md §4.2): does the user's role
 * unconditionally grant a permission, with no scoping needed?
 *
 * NOT YET COMPLETE: the full per-role default-permission catalog lives in the "Roles &
 * Permissions" document referenced by 02-prd.md §6, which is not present in this repository.
 * Only the roles/keys explicitly called out elsewhere in the docs are seeded below (e.g.
 * CHIEF_LIBRARIAN's unconditional fine-waiver authority per 03-srs.md [LIB-006] — "The Chief
 * Librarian may waive a fine", no scoping mentioned, unlike Library Assistants who need a
 * threshold-scoped grant). Everything else falls through to Layer 2 (scoped permission_grant) or
 * Layer 3 (deny). Extend ROLE_PERMISSIONS as each module's permission keys and role defaults are
 * specified.
 */
@Component
public class RoleTemplate {

    private static final String WILDCARD = "*";

    private static final Map<String, Set<String>> ROLE_PERMISSIONS = Map.of(
            "SUPER_ADMIN", Set.of(WILDCARD),
            "ADMIN", Set.of(WILDCARD),
            "CHIEF_LIBRARIAN", Set.of("LIBRARY.FINE_WAIVER"));

    public boolean grants(String role, String permissionKey) {
        Set<String> granted = ROLE_PERMISSIONS.get(role);
        return granted != null && (granted.contains(WILDCARD) || granted.contains(permissionKey));
    }
}
