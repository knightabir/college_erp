package com.college.erp.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.college.erp.module.admin.service.UserManagementService;

import lombok.RequiredArgsConstructor;

/**
 * Seeds a single "default" tenant plus its SUPER_ADMIN account on startup if none exists yet —
 * air-gap/on-prem, single-college deployments only (see {@code ErpProperties.Bootstrap}'s javadoc).
 * Multi-tenant cloud/SaaS deployments rely on {@code POST /api/v1/register} instead (see
 * {@code com.college.erp.tenant.controller.RegistrationController}) — set
 * {@code erp.bootstrap.admin.enabled=false} there.
 *
 * <p>Deliberately a thin wrapper — all business logic, and the {@code module.auth} repository
 * access it needs, lives in {@code module.admin}'s {@link UserManagementService}, which already
 * carries the documented, ArchUnit-allowlisted exception for that (see {@code ModuleBoundaryTest}).
 * This class itself depends on a module *service*, the same sanctioned direction
 * {@code scheduler.jobs.*} already uses to call into module services from kernel code — it isn't a
 * {@code module.*} package, so the "no cross-module repository access" rule doesn't apply to it
 * regardless.
 *
 * <p>Runs as an {@link ApplicationRunner}, i.e. after the full context (including Flyway's
 * migration run) has started — no separate "is the schema ready yet" handling is needed.
 */
@Component
@RequiredArgsConstructor
public class BootstrapAdminRunner implements ApplicationRunner {

    private final ErpProperties erpProperties;
    private final UserManagementService userManagementService;

    @Override
    public void run(ApplicationArguments args) {
        ErpProperties.Bootstrap.Admin adminConfig = erpProperties.getBootstrap().getAdmin();
        if (!adminConfig.isEnabled()) {
            return;
        }
        ErpProperties.Bootstrap.Tenant tenantConfig = erpProperties.getBootstrap().getTenant();
        userManagementService.bootstrapTenantAndSuperAdminIfMissing(
                tenantConfig.getName(), tenantConfig.getCode(), tenantConfig.getContactEmail(),
                adminConfig.getUsername(), adminConfig.getEmail(), adminConfig.getFullName(),
                adminConfig.getPassword());
    }
}
