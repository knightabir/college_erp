package com.college.erp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

/**
 * Typed jwt.* config — never scattered @Value fields for a multi-property group
 * (07-coding-standards.md §3.3). Bound from JWT_SECRET / JWT_ACCESS_EXPIRY / JWT_REFRESH_EXPIRY
 * (08-system-architecture.md §13.2).
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** HS256 secret, must be >= 256 bits. Dev-only default — override via JWT_SECRET in every real deployment. */
    private String secret = "dev-only-secret-key-change-me-must-be-at-least-256-bits-long!!";

    /** Access token TTL in ms. Default 15 minutes. */
    private long accessExpiry = 900_000L;

    /** Refresh token TTL in ms. Default 7 days. */
    private long refreshExpiry = 604_800_000L;

    private String issuer = "college-erp";
}
