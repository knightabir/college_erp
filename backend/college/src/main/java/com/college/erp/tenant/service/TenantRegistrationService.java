package com.college.erp.tenant.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.DuplicateResourceException;
import com.college.erp.config.JwtProperties;
import com.college.erp.module.admin.service.UserManagementService;
import com.college.erp.module.auth.service.RefreshTokenService;
import com.college.erp.security.jwt.JwtTokenProvider;
import com.college.erp.tenant.domain.Tenant;
import com.college.erp.tenant.domain.TenantRepository;
import com.college.erp.tenant.domain.TenantStatus;
import com.college.erp.tenant.dto.RegisterCollegeRequest;
import com.college.erp.tenant.dto.RegisterCollegeResponse;
import com.college.erp.tenant.dto.TenantDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * The one public, unauthenticated entry point into this whole system (see
 * RegistrationController) — "register your college" creates a new {@link Tenant} plus that
 * tenant's own SUPER_ADMIN account, in one transaction, and logs the new admin straight in.
 *
 * <p>Deliberately a kernel service (not {@code module.tenant.service}) depending on
 * {@code module.admin}'s {@code UserManagementService} and {@code module.auth}'s
 * {@code RefreshTokenService} — the same sanctioned "kernel calls into a module's service"
 * direction {@code scheduler.jobs.*} and {@code BootstrapAdminRunner} already use (see
 * ModuleBoundaryTest's javadoc); {@code com.college.erp.tenant} isn't a {@code module.*} package,
 * so the "no cross-module repository access" rule doesn't apply to it either way, but this class
 * still goes through those modules' services rather than their repositories on principle.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantRegistrationService {

    private static final String MODULE = "TENANT";

    private final TenantRepository tenantRepository;
    private final UserManagementService userManagementService;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;
    private final AuditLogWriter auditLogWriter;

    @Transactional
    public RegisterCollegeResponse register(RegisterCollegeRequest request, String ipAddress, String deviceInfo) {
        String code = request.getCollegeCode().trim().toLowerCase();
        if (tenantRepository.existsByCodeAndDeletedFalse(code)) {
            throw new DuplicateResourceException("College code '" + code + "' is already taken");
        }

        Tenant tenant = Tenant.builder()
                .name(request.getCollegeName())
                .code(code)
                .affiliatedUniversity(request.getAffiliatedUniversity())
                .accreditation(request.getAccreditation())
                .establishedYear(request.getEstablishedYear())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .contactEmail(request.getCollegeContactEmail())
                .contactPhone(request.getCollegeContactPhone())
                .website(request.getWebsite())
                .status(TenantStatus.ACTIVE)
                .build();
        Tenant savedTenant = tenantRepository.save(tenant);

        // createTenantOwner() (module.admin) handles its own username/email duplicate checks —
        // a DuplicateResourceException there rolls back the Tenant insert above too, same
        // transaction, so a failed registration never leaves an orphaned, admin-less tenant.
        UUID adminUserId = userManagementService.createTenantOwner(savedTenant.getId(),
                request.getAdminUsername(), request.getAdminEmail(), request.getAdminFullName(),
                request.getAdminPassword());

        String accessToken = jwtTokenProvider.generateAccessToken(
                adminUserId, request.getAdminUsername(), request.getAdminEmail());
        String refreshToken = refreshTokenService.issue(adminUserId, deviceInfo, ipAddress);

        auditLogWriter.write(AuditAction.CREATE, MODULE, "Tenant", savedTenant.getId().toString(),
                "College registered: " + savedTenant.getName() + " (" + savedTenant.getCode() + ")");
        log.info("New tenant registered: code=[{}] name=[{}]", savedTenant.getCode(), savedTenant.getName());

        return RegisterCollegeResponse.builder()
                .tenant(TenantDto.from(savedTenant))
                .adminUserId(adminUserId)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtProperties.getAccessExpiry() / 1000)
                .build();
    }
}
