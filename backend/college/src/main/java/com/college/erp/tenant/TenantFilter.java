package com.college.erp.tenant;

import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.college.erp.security.jwt.UserPrincipal;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Runs immediately after {@code JwtAuthenticationFilter} in the chain (see SecurityConfig) — by
 * the time this filter runs, {@code SecurityContextHolder} already holds a fully-resolved
 * {@link UserPrincipal} (tenantId included, loaded fresh from the DB the same way roles are —
 * see {@code UserDetailsServiceImpl}), so this filter's only job is copying that tenant id into
 * {@link TenantContext} for the rest of the request to read (service-layer entity creation,
 * {@link TenantAwareDataSource}'s per-connection RLS session variable).
 *
 * <p>Public/pre-authentication endpoints (login, refresh, the college-registration endpoint) have
 * no authenticated principal at this point — {@link TenantContext} is simply left unset for those
 * requests, which is correct: registration is precisely the request that *creates* a tenant, so
 * there is nothing to scope it to yet.
 *
 * <p>Always clears {@link TenantContext} in a {@code finally} block — Tomcat reuses request-handling
 * threads across requests, so a forgotten clear would leak one request's tenant into a later,
 * unrelated request that happens to land on the same thread. Same reasoning as
 * {@code JwtAuthenticationFilter}'s own MDC cleanup.
 *
 * <p>Deliberately NOT a {@code @Component} — wired explicitly via
 * {@code SecurityConfig#addFilterAfter}, same reasoning as {@code JwtAuthenticationFilter}
 * (auto-registration would also add it a second time as a plain servlet filter).
 */
public class TenantFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal
                    && principal.getTenantId() != null) {
                TenantContext.setCurrentTenantId(principal.getTenantId());
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
