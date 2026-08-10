package com.college.erp.tenant.dto;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

/**
 * Standard "sign up and land inside your own workspace" SaaS response — the new tenant's details
 * plus a ready-to-use access/refresh token pair for its brand-new SUPER_ADMIN, exactly what
 * AuthService.login() would return on a subsequent login, so the frontend never has to make a
 * second round trip just to log the admin in after registering.
 */
@Getter
@Builder
public class RegisterCollegeResponse {

    private TenantDto tenant;
    private UUID adminUserId;
    private String accessToken;
    private String refreshToken;
    private long expiresIn;
}
