package com.college.erp.security.jwt;

import java.util.Collection;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.Getter;

/**
 * Spring Security UserDetails implementation. Deliberately does not import the module.auth
 * User entity — the security kernel must not depend on a feature module (module boundary rules,
 * 08-system-architecture.md §3.1). UserDetailsServiceImpl (in module.auth) maps User -> this via
 * the static factory below.
 */
@Getter
public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final UUID tenantId;
    private final String username;
    private final String email;
    private final String passwordHash;
    private final boolean enabled;
    private final boolean accountNonLocked;
    private final Collection<? extends GrantedAuthority> authorities;

    private UserPrincipal(UUID id, UUID tenantId, String username, String email, String passwordHash,
            boolean enabled, boolean accountNonLocked, Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.tenantId = tenantId;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.enabled = enabled;
        this.accountNonLocked = accountNonLocked;
        this.authorities = authorities;
    }

    /** {@code tenantId} — read by {@code com.college.erp.tenant.TenantFilter} to populate
     *  {@code TenantContext} for the rest of the request (multi-tenancy, 08-system-architecture.md
     *  §5.2). Loaded fresh from the DB on every request the same way roles/authorities already are
     *  (ADR-004) — never embedded in the JWT itself. */
    public static UserPrincipal create(UUID id, UUID tenantId, String username, String email, String passwordHash,
            boolean enabled, boolean accountNonLocked, Collection<? extends GrantedAuthority> authorities) {
        return new UserPrincipal(id, tenantId, username, email, passwordHash, enabled, accountNonLocked, authorities);
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
