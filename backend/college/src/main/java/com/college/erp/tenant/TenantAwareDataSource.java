package com.college.erp.tenant;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.DelegatingDataSource;

import lombok.extern.slf4j.Slf4j;

/**
 * Makes Postgres Row-Level Security (08-system-architecture.md §5.2) actually work under a
 * connection pool. RLS policies key off the {@code app.current_tenant} session variable
 * (see V017__multi_tenancy_foundation.sql's policies), but HikariCP freely hands the *same*
 * physical connection to different requests/threads over its lifetime and does **not** reset
 * session-level state between checkouts — without this wrapper, a connection that served tenant
 * A's request could still carry tenant A's session variable when handed to tenant B's request
 * next, silently leaking data.
 *
 * <p>Fix: intercept every {@code getConnection()} call (a fresh physical connection from the pool,
 * or a reused one — indistinguishable from here, so always act) and immediately set the session
 * variable from {@link TenantContext} before the connection is used for anything. If no tenant is
 * in context (Flyway migrations, the pre-authentication registration/login endpoints, the startup
 * bootstrap runner, scheduled jobs not yet made tenant-aware), {@code RESET} it instead. The intent
 * is the same fail-closed default either way: a code path that forgot to establish tenant context
 * should see zero rows on every RLS-protected table, never another tenant's data.
 *
 * <p><strong>{@code RESET} does not reliably produce {@code NULL} here</strong> — this bit
 * V017/V018 in practice, not just in theory. {@code app.current_tenant} is a custom (placeholder)
 * GUC, and Postgres's placeholder-GUC semantics mean {@code RESET} only returns
 * {@code current_setting(..., true)} to {@code NULL} on a connection where the variable was never
 * {@code SET} at all in that session. Once *any* prior checkout on this same pooled connection has
 * executed {@code SET app.current_tenant = '...'} (i.e. served a tenant-scoped request), a later
 * {@code RESET} on that same physical connection instead settles it to {@code ''} (empty string)
 * for the rest of that connection's life — confirmed live: a scheduled job (no tenant context)
 * picking up a connection that had just served a tenant's request failed with
 * {@code invalid input syntax for type uuid: ""} querying subject_offerings, not the expected
 * empty result. Rather than fight Postgres's GUC reset semantics here (there is no reliable way
 * from a plain {@code RESET}/{@code SET} to force true "unset" on a connection that has ever held a
 * value), V018 fixed every affected policy to wrap the read side in
 * {@code NULLIF(current_setting('app.current_tenant', true), '')} before the {@code ::UUID} cast —
 * both "never set" and "reset after being set" now collapse to {@code NULL} before casting, so the
 * comparison is unknown/false (same fail-closed result) instead of throwing. Keep that NULLIF
 * wrapper in any future policy added against {@code app.current_tenant}.
 *
 * <p>String-concatenating the tenant id into the SQL text (Postgres's {@code SET}/{@code RESET}
 * cannot take a JDBC {@code ?} placeholder — it is not a regular DML statement) is safe here
 * specifically because the value is always a real {@link UUID}'s {@code toString()}, whose format
 * is guaranteed by the JDK and can never contain a quote character or otherwise escape the literal.
 *
 * <p>Registered as the app's primary {@code DataSource} bean (see {@code TenantDataSourceConfig}),
 * built from {@code spring.datasource.*} — i.e. used by JPA/Hibernate only. Flyway deliberately
 * does <strong>not</strong> go through this bean at all: it has its own independent connection
 * configured via {@code spring.flyway.url/user/password} (a separate, standard Spring Boot Flyway
 * property group), connecting as a superuser locally. That split exists because Postgres always
 * exempts superusers/table-owners from their own RLS policies regardless of any session variable —
 * migrations (including V017, which needs to {@code CREATE ROLE}/{@code GRANT}) need that elevated
 * connection, while normal request traffic must run as the deliberately non-superuser
 * {@code erp_app} role for RLS to actually restrict anything. See {@code V017}'s header comment for
 * the full role split.
 */
@Slf4j
public class TenantAwareDataSource extends DelegatingDataSource {

    private static final String SESSION_VARIABLE = "app.current_tenant";

    public TenantAwareDataSource(DataSource targetDataSource) {
        super(targetDataSource);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return applyTenantContext(obtainTargetDataSource().getConnection());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return applyTenantContext(obtainTargetDataSource().getConnection(username, password));
    }
    // obtainTargetDataSource() itself is inherited from DelegatingDataSource — already does
    // exactly what's needed (return the configured target, or throw IllegalStateException if
    // none is set), no need to redeclare it.

    private Connection applyTenantContext(Connection connection) throws SQLException {
        UUID tenantId = TenantContext.getCurrentTenantId();
        try (Statement statement = connection.createStatement()) {
            if (tenantId != null) {
                statement.execute("SET " + SESSION_VARIABLE + " = '" + tenantId + "'");
            } else {
                statement.execute("RESET " + SESSION_VARIABLE);
            }
        } catch (SQLException ex) {
            // Never hand back a connection whose tenant session state we couldn't confirm — fail
            // the checkout rather than risk one request silently running with another's context.
            log.error("Failed to apply tenant context to a checked-out connection", ex);
            connection.close();
            throw ex;
        }
        return connection;
    }
}
