package com.college.erp.module.auth.service;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.BusinessRuleViolationException;
import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.config.ErpProperties;
import com.college.erp.config.JwtProperties;
import com.college.erp.module.auth.domain.RefreshToken;
import com.college.erp.module.auth.domain.RoleAssignmentRepository;
import com.college.erp.module.auth.domain.User;
import com.college.erp.module.auth.domain.UserRepository;
import com.college.erp.module.auth.dto.ChangePasswordRequest;
import com.college.erp.module.auth.dto.LoginRequest;
import com.college.erp.module.auth.dto.LoginResponse;
import com.college.erp.module.auth.dto.TokenResponse;
import com.college.erp.security.jwt.JwtTokenProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * login/refresh/logout/change-password. Follows the authentication flow in
 * 08-system-architecture.md §4.1 exactly: load user, BCrypt verify, check enabled/locked, load
 * active roles, issue JWT + refresh token, audit LOGIN async.
 *
 * NOT YET IMPLEMENTED: TOTP verification (FR-AUTH-4). A user with totp_enabled = true fails
 * login with a clear error rather than silently skipping the second factor — see loginTotpGuard.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String MODULE = "AUTH";

    private final UserRepository userRepository;
    private final RoleAssignmentRepository roleAssignmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final ErpProperties erpProperties;
    private final RefreshTokenService refreshTokenService;
    private final AuditLogWriter auditLogWriter;

    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress, String deviceInfo) {
        User user = userRepository.findByUsernameOrEmail(request.getIdentifier())
                .orElseThrow(() -> invalidCredentials(null, request.getIdentifier()));

        unlockIfLockoutExpired(user);
        if (user.isLocked()) {
            throw new BusinessRuleViolationException(
                    "Account is locked due to repeated failed login attempts. Try again later.");
        }

        loginTotpGuard(user);

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            registerFailedAttempt(user);
            throw invalidCredentials(user.getId(), request.getIdentifier());
        }

        if (!user.isEnabled()) {
            throw new BusinessRuleViolationException("Account is disabled");
        }

        user.setFailedLoginAttempts(0);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        Set<String> roles = activeRoleNames(user.getId());
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), user.getEmail());
        String refreshToken = refreshTokenService.issue(user.getId(), deviceInfo, ipAddress);

        auditLogWriter.write(AuditAction.LOGIN, MODULE, "User", user.getId().toString(),
                "Login: " + user.getUsername());
        log.info("Login succeeded for user [{}]", user.getUsername());

        return LoginResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .roles(roles)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtProperties.getAccessExpiry() / 1000)
                .build();
    }

    @Transactional
    public TokenResponse refresh(String rawRefreshToken) {
        RefreshToken token = refreshTokenService.findValid(rawRefreshToken)
                .orElseThrow(() -> new BusinessRuleViolationException("Invalid or expired refresh token"));

        User user = userRepository.findById(token.getUserId())
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("User", token.getUserId()));

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), user.getEmail());

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .expiresIn(jwtProperties.getAccessExpiry() / 1000)
                .build();
    }

    /** Idempotent — logging out an already-invalid/unknown token is not an error. */
    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.findValid(rawRefreshToken).ifPresent(refreshTokenService::revoke);
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessRuleViolationException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);

        auditLogWriter.write(AuditAction.PASSWORD_CHANGED, MODULE, "User", userId.toString(),
                "Password changed for user: " + user.getUsername());
        log.info("Password changed for user [{}]", user.getUsername());
    }

    private void loginTotpGuard(User user) {
        if (user.isTotpEnabled()) {
            throw new BusinessRuleViolationException(
                    "TOTP-protected accounts cannot log in yet — 2FA verification is not implemented.");
        }
    }

    private void unlockIfLockoutExpired(User user) {
        if (user.isLocked() && user.getLockedUntil() != null && Instant.now().isAfter(user.getLockedUntil())) {
            user.setLocked(false);
            user.setLockedUntil(null);
            user.setFailedLoginAttempts(0);
            userRepository.save(user);
        }
    }

    private void registerFailedAttempt(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= erpProperties.getSecurity().getMaxFailedAttempts()) {
            user.setLocked(true);
            user.setLockedUntil(Instant.now().plusSeconds(erpProperties.getSecurity().getLockoutMinutes() * 60L));
            auditLogWriter.write(AuditAction.ACCOUNT_LOCKED, MODULE, "User", user.getId().toString(),
                    "Account locked after " + attempts + " failed attempts: " + user.getUsername());
            log.warn("Account locked: user [{}] after {} failed attempts", user.getUsername(), attempts);
        }

        userRepository.save(user);
        auditLogWriter.write(AuditAction.LOGIN_FAILED, MODULE, "User", user.getId().toString(),
                "Failed login attempt for user: " + user.getUsername());
    }

    private BusinessRuleViolationException invalidCredentials(UUID userId, String identifier) {
        if (userId == null) {
            auditLogWriter.write(AuditAction.LOGIN_FAILED, MODULE, "User", null,
                    "Failed login attempt for unknown identifier: " + identifier);
        }
        log.info("Login attempt failed for identifier [{}]", identifier);
        // Deliberately generic — never reveal whether the identifier itself exists.
        return new BusinessRuleViolationException("Invalid username or password");
    }

    private Set<String> activeRoleNames(UUID userId) {
        return roleAssignmentRepository.findActiveByUserId(userId).stream()
                .map(ra -> ra.getRole().name())
                .collect(Collectors.toUnmodifiableSet());
    }
}
