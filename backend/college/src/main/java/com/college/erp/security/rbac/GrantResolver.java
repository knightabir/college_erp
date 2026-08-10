package com.college.erp.security.rbac;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Layer 2 of permission evaluation (08-system-architecture.md §4.2): does the user hold an
 * active, non-expired, scoped permission_grant that covers this permission key + scope?
 *
 * Aggregates ALL registered {@link PermissionGrantSource} beans, not just one — the schema has
 * two functionally-identical grant tables (module.auth's general permission_grants and
 * module.finance's accountant_permission_grants, per 04-database-schema.md §5.3 vs §10.8), and a
 * user's active grants can legitimately come from either. Spring injects an empty list when no
 * source beans exist yet, so no NoOp placeholder is needed here (unlike the single-source seams
 * elsewhere, e.g. UserDetailsService) — the kernel is startable with zero sources registered.
 */
@Component
@RequiredArgsConstructor
public class GrantResolver {

    private final List<PermissionGrantSource> grantSources;

    public boolean hasActiveGrant(UUID userId, String permissionKey, PermissionScope scope) {
        return grantSources.stream()
                .flatMap(source -> source.findActiveGrants(userId).stream())
                .filter(g -> g.permissionKey().equals(permissionKey))
                .anyMatch(g -> g.coversScope(scope));
    }
}
