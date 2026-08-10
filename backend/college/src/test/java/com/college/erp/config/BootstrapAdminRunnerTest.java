package com.college.erp.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.college.erp.module.admin.service.UserManagementService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** See BootstrapAdminRunner's own javadoc for what this solves (the auth bootstrap problem). */
@ExtendWith(MockitoExtension.class)
class BootstrapAdminRunnerTest {

    @Mock
    private UserManagementService userManagementService;

    @Test
    @DisplayName("run() — enabled: delegates to bootstrapTenantAndSuperAdminIfMissing() with the "
            + "configured tenant + admin credentials")
    void run_enabled_delegatesWithConfiguredCredentials() throws Exception {
        // ARRANGE
        ErpProperties erpProperties = new ErpProperties();
        erpProperties.getBootstrap().getAdmin().setEnabled(true);
        erpProperties.getBootstrap().getAdmin().setUsername("superadmin");
        erpProperties.getBootstrap().getAdmin().setEmail("superadmin@college.edu");
        erpProperties.getBootstrap().getAdmin().setFullName("System Administrator");
        erpProperties.getBootstrap().getAdmin().setPassword("ChangeMe@123");
        erpProperties.getBootstrap().getTenant().setName("Default College");
        erpProperties.getBootstrap().getTenant().setCode("default");
        erpProperties.getBootstrap().getTenant().setContactEmail("admin@college.edu");
        BootstrapAdminRunner runner = new BootstrapAdminRunner(erpProperties, userManagementService);

        // ACT
        runner.run(null);

        // ASSERT
        verify(userManagementService).bootstrapTenantAndSuperAdminIfMissing(
                "Default College", "default", "admin@college.edu",
                "superadmin", "superadmin@college.edu", "System Administrator", "ChangeMe@123");
    }

    @Test
    @DisplayName("run() — disabled via erp.bootstrap.admin.enabled=false: never calls the service")
    void run_disabled_skipsEntirely() throws Exception {
        // ARRANGE
        ErpProperties erpProperties = new ErpProperties();
        erpProperties.getBootstrap().getAdmin().setEnabled(false);
        BootstrapAdminRunner runner = new BootstrapAdminRunner(erpProperties, userManagementService);

        // ACT
        runner.run(null);

        // ASSERT
        verify(userManagementService, never()).bootstrapTenantAndSuperAdminIfMissing(
                any(), any(), any(), any(), any(), any(), any());
    }
}
