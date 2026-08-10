package com.college.erp.security.rbac;

import java.io.Serializable;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import com.college.erp.security.jwt.UserPrincipal;

import lombok.RequiredArgsConstructor;

/**
 * Three-layer permission evaluation registered as Spring Security's method-security
 * PermissionEvaluator (08-system-architecture.md §4.2):
 *   1. Role template — does the role unconditionally grant this permission?
 *   2. Scoped grant — does the user hold an active permission_grant covering it?
 *   3. Deny.
 *
 * For scope-aware checks beyond "any grant with this key", modules should call GrantResolver
 * directly from a per-entity *SecurityService (e.g. StudentSecurityService.isOwnProfile), the
 * pattern used throughout 07-coding-standards.md §5.1 — this generic hasPermission(...) form
 * only has the permission key to go on, not a resolved PermissionScope.
 */
@Component
@RequiredArgsConstructor
public class PermissionEvaluator implements org.springframework.security.access.PermissionEvaluator {

    private final RoleTemplate roleTemplate;
    private final GrantResolver grantResolver;

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        return evaluate(authentication, String.valueOf(permission));
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType,
            Object permission) {
        return evaluate(authentication, String.valueOf(permission));
    }

    private boolean evaluate(Authentication authentication, String permissionKey) {
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return false;
        }

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String role = authority.getAuthority().replaceFirst("^ROLE_", "");
            if (roleTemplate.grants(role, permissionKey)) {
                return true;
            }
        }

        UUID userId = principal.getId();
        return grantResolver.hasActiveGrant(userId, permissionKey, PermissionScope.none());
    }
}
