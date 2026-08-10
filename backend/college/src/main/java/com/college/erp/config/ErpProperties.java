package com.college.erp.config;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

/**
 * Typed erp.* config, grouped by concern (07-coding-standards.md §3.3). "security"
 * (account lockout) and "finance" (segregation of duties) are populated so far —
 * storage/notification/totp groups will be added alongside the modules that need them.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "erp")
public class ErpProperties {

    private final Security security = new Security();
    private final Finance finance = new Finance();
    private final Asset asset = new Asset();
    private final Notification notification = new Notification();
    private final Storage storage = new Storage();
    private final Bootstrap bootstrap = new Bootstrap();

    @Getter
    @Setter
    public static class Security {
        /** FR-AUTH-3: lock the account after this many consecutive failed attempts. */
        private int maxFailedAttempts = 5;
        /** FR-AUTH-3: lockout duration once maxFailedAttempts is reached. */
        private int lockoutMinutes = 30;
    }

    /** Solves the auth bootstrap problem for **air-gap/on-prem, single-college deployments**
     *  (08-system-architecture.md §5.2): those never use the public {@code POST /api/v1/register}
     *  self-service flow (see {@code RegistrationController}) — there's only ever one college on
     *  such a deployment — so something still has to create that one college's first account. See
     *  {@code BootstrapAdminRunner} / {@code UserManagementService
     *  .bootstrapTenantAndSuperAdminIfMissing()} for the fix: a one-time, idempotent startup check
     *  that seeds a single "default" {@code Tenant} plus its SUPER_ADMIN if none exists yet. Change
     *  every default below via env vars before any non-local deployment — the account is created
     *  with mustChangePassword=true regardless, matching UserManagementService.create()'s existing
     *  convention for admin-issued passwords. For **cloud/SaaS deployments** where colleges
     *  self-register instead, set {@code BOOTSTRAP_ADMIN_ENABLED=false} — otherwise this creates an
     *  unwanted extra "Default College" tenant alongside real signups on the very first startup. */
    @Getter
    @Setter
    public static class Bootstrap {
        private final Admin admin = new Admin();
        private final Tenant tenant = new Tenant();

        @Getter
        @Setter
        public static class Admin {
            /** Set to false for cloud/SaaS deployments relying on self-registration instead — see
             *  the enclosing class's javadoc. Once ANY SUPER_ADMIN exists anywhere (including one
             *  created via self-registration), this check is a no-op regardless, so leaving it
             *  {@code true} is harmless past the very first startup — just occasionally redundant. */
            private boolean enabled = true;
            private String username = "superadmin";
            private String email = "superadmin@college.edu";
            private String fullName = "System Administrator";
            /** Must satisfy the same >= 8 char minimum ChangePasswordRequest enforces. Never leave
             *  this default in place outside local/dev use — it is publicly documented (README,
             *  Postman collection). */
            private String password = "ChangeMe@123";
        }

        /** The "college details" for the single bootstrap tenant created alongside
         *  Bootstrap.Admin above — same air-gap/on-prem-only reasoning. */
        @Getter
        @Setter
        public static class Tenant {
            private String name = "Default College";
            private String code = "default";
            private String contactEmail = "admin@college.edu";
        }
    }

    @Getter
    @Setter
    public static class Finance {
        /** FR-RBAC-5 / SegregationOfDutiesGuard: above this amount, the same user may not both
         *  create and approve a financial transaction. */
        private BigDecimal segregationOfDutiesThreshold = BigDecimal.valueOf(100_000);
    }

    @Getter
    @Setter
    public static class Asset {
        /** 03-srs.md [AST-004]: above this purchase cost, disposal needs an Admin, not just an
         *  Asset Manager — see AssetTransferService.dispose() javadoc for how the "Asset Manager
         *  initiates + Admin confirms" two-party rule is modeled given the schema has no
         *  pending-disposal state to hold an in-flight approval. */
        private BigDecimal disposalValueThreshold = BigDecimal.valueOf(50_000);
    }

    @Getter
    @Setter
    public static class Notification {
        /** 06-design-document.md §8.1: EmailChannel.isEnabled(). No SMTP client is wired up yet
         *  (see notification.channel.EmailChannel javadoc) — false by default, matching
         *  air-gap-first (ADR-001). */
        private boolean emailEnabled = false;
        /** 06-design-document.md §8.1: SmsChannel.isEnabled() — "false in air-gap profile". */
        private boolean smsEnabled = false;
    }

    @Getter
    @Setter
    public static class Storage {
        /** 06-design-document.md §9.3: local | minio | s3 — only "local" has an implementation
         *  (see LocalFileStorageService javadoc for why minio/s3 are deferred). */
        private String mode = "local";
        private final Local local = new Local();

        @Getter
        @Setter
        public static class Local {
            private String basePath = "./uploads";
        }
    }
}
