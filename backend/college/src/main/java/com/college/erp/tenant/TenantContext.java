package com.college.erp.tenant;

import java.util.UUID;

/**
 * Ambient "which tenant is the current thread acting on behalf of" holder — the tenant-scoping
 * equivalent of {@code SecurityContextHolder}, following the same ThreadLocal pattern already used
 * throughout this codebase (e.g. {@code AuditLogWriter} reads {@code SecurityContextHolder}/
 * {@code MDC} the same ambient way rather than threading a "current user" parameter through every
 * service method).
 *
 * <p>Set once per HTTP request by {@link TenantFilter} (from the authenticated principal's own
 * tenant, resolved fresh from the DB — see {@code UserDetailsServiceImpl}), read by
 * {@code module.academic}/{@code module.student}'s {@code *Service.create()} methods when
 * populating a new entity's {@code tenantId}, and by {@link TenantAwareDataSource} on every
 * physical connection checkout to set/reset Postgres's {@code app.current_tenant} session variable
 * for Row-Level Security. Always cleared in a {@code finally} block at the end of the request —
 * Tomcat reuses request-handling threads across requests, so a forgotten clear would leak one
 * request's tenant into a completely unrelated later request on the same thread.
 *
 * <p><strong>Not visible across an {@code @Async} hop</strong> (a different thread runs the async
 * method) — same caveat {@code AuditLogWriter} already documents for {@code SecurityContextHolder}/
 * {@code MDC}. No currently-converted table is written from an async path, so this doesn't matter
 * yet; if a future module's async code needs tenant context, capture
 * {@code TenantContext.getCurrentTenantId()} synchronously before the hop and pass it as an
 * explicit method parameter, the same way {@code AuditLogWriter.write()} already does for its own
 * ThreadLocal reads.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setCurrentTenantId(UUID tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static UUID getCurrentTenantId() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
