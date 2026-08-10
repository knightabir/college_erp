package com.college.erp.module.auth.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.BusinessRuleViolationException;
import com.college.erp.config.ErpProperties;
import com.college.erp.config.JwtProperties;
import com.college.erp.module.auth.domain.RefreshToken;
import com.college.erp.module.auth.domain.RoleAssignmentRepository;
import com.college.erp.module.auth.domain.User;
import com.college.erp.module.auth.domain.UserRepository;
import com.college.erp.module.auth.dto.ChangePasswordRequest;
import com.college.erp.module.auth.dto.LoginRequest;
import com.college.erp.module.auth.dto.LoginResponse;
import com.college.erp.security.jwt.JwtTokenProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleAssignmentRepository roleAssignmentRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private AuditLogWriter auditLogWriter;

    private JwtProperties jwtProperties;
    private ErpProperties erpProperties;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        erpProperties = new ErpProperties();
        erpProperties.getSecurity().setMaxFailedAttempts(3);
        erpProperties.getSecurity().setLockoutMinutes(30);

        authService = new AuthService(userRepository, roleAssignmentRepository, passwordEncoder,
                jwtTokenProvider, jwtProperties, erpProperties, refreshTokenService, auditLogWriter);
    }

    private User buildUser() {
        // id lives on BaseEntity, out of reach of User's own @Builder (07-coding-standards.md
        // §2.2 — the builder only covers a class's own fields, id/timestamps are set separately).
        User user = User.builder()
                .username("jdoe")
                .email("jdoe@college.edu")
                .passwordHash("hashed-password")
                .fullName("Jane Doe")
                .enabled(true)
                .locked(false)
                .failedLoginAttempts(0)
                .totpEnabled(false)
                .build();
        user.setId(UUID.randomUUID());
        return user;
    }

    @Test
    @DisplayName("login() — correct credentials returns tokens and resets failed attempts")
    void login_success_returnsTokens() {
        // ARRANGE
        User user = buildUser();
        LoginRequest request = new LoginRequest();
        request.setIdentifier("jdoe");
        request.setPassword("correct-password");

        when(userRepository.findByUsernameOrEmail("jdoe")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct-password", "hashed-password")).thenReturn(true);
        when(roleAssignmentRepository.findActiveByUserId(user.getId())).thenReturn(List.of());
        when(jwtTokenProvider.generateAccessToken(user.getId(), "jdoe", "jdoe@college.edu"))
                .thenReturn("access-token");
        when(refreshTokenService.issue(eq(user.getId()), anyString(), anyString()))
                .thenReturn("refresh-token");

        // ACT
        LoginResponse response = authService.login(request, "127.0.0.1", "test-agent");

        // ASSERT
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(user.getFailedLoginAttempts()).isZero();
        verify(auditLogWriter).write(eq(AuditAction.LOGIN), anyString(), eq("User"),
                eq(user.getId().toString()), anyString());
    }

    @Test
    @DisplayName("login() — unknown identifier throws a generic invalid-credentials error")
    void login_unknownIdentifier_throwsBusinessRuleViolation() {
        // ARRANGE
        when(userRepository.findByUsernameOrEmail("ghost")).thenReturn(Optional.empty());
        LoginRequest request = new LoginRequest();
        request.setIdentifier("ghost");
        request.setPassword("whatever");

        // ACT / ASSERT
        assertThatThrownBy(() -> authService.login(request, "127.0.0.1", "agent"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Invalid username or password");
    }

    @Test
    @DisplayName("login() — wrong password increments failed attempts without locking below threshold")
    void login_wrongPassword_incrementsFailedAttempts() {
        // ARRANGE
        User user = buildUser();
        user.setFailedLoginAttempts(0);
        LoginRequest request = new LoginRequest();
        request.setIdentifier("jdoe");
        request.setPassword("wrong-password");

        when(userRepository.findByUsernameOrEmail("jdoe")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        // ACT / ASSERT
        assertThatThrownBy(() -> authService.login(request, "127.0.0.1", "agent"))
                .isInstanceOf(BusinessRuleViolationException.class);

        assertThat(user.getFailedLoginAttempts()).isEqualTo(1);
        assertThat(user.isLocked()).isFalse();
        verify(auditLogWriter, never()).write(eq(AuditAction.ACCOUNT_LOCKED), any(), any(), any(), any());
    }

    @Test
    @DisplayName("login() — wrong password at the configured threshold locks the account")
    void login_wrongPassword_reachesThreshold_locksAccount() {
        // ARRANGE — one failed attempt away from the max of 3
        User user = buildUser();
        user.setFailedLoginAttempts(2);
        LoginRequest request = new LoginRequest();
        request.setIdentifier("jdoe");
        request.setPassword("wrong-password");

        when(userRepository.findByUsernameOrEmail("jdoe")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        // ACT / ASSERT
        assertThatThrownBy(() -> authService.login(request, "127.0.0.1", "agent"))
                .isInstanceOf(BusinessRuleViolationException.class);

        assertThat(user.isLocked()).isTrue();
        assertThat(user.getLockedUntil()).isAfter(Instant.now());
        verify(auditLogWriter).write(eq(AuditAction.ACCOUNT_LOCKED), anyString(), eq("User"),
                eq(user.getId().toString()), anyString());
    }

    @Test
    @DisplayName("login() — locked account with lockout window still active is rejected")
    void login_accountLocked_notExpired_throws() {
        // ARRANGE
        User user = buildUser();
        user.setLocked(true);
        user.setLockedUntil(Instant.now().plusSeconds(600));
        when(userRepository.findByUsernameOrEmail("jdoe")).thenReturn(Optional.of(user));
        LoginRequest request = new LoginRequest();
        request.setIdentifier("jdoe");
        request.setPassword("correct-password");

        // ACT / ASSERT
        assertThatThrownBy(() -> authService.login(request, "127.0.0.1", "agent"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("locked");
    }

    @Test
    @DisplayName("login() — lockout window expired auto-unlocks and login proceeds")
    void login_accountLockExpired_unlocksAndProceeds() {
        // ARRANGE
        User user = buildUser();
        user.setLocked(true);
        user.setLockedUntil(Instant.now().minusSeconds(60)); // expired
        LoginRequest request = new LoginRequest();
        request.setIdentifier("jdoe");
        request.setPassword("correct-password");

        when(userRepository.findByUsernameOrEmail("jdoe")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct-password", "hashed-password")).thenReturn(true);
        when(roleAssignmentRepository.findActiveByUserId(user.getId())).thenReturn(List.of());
        when(jwtTokenProvider.generateAccessToken(any(), anyString(), anyString())).thenReturn("access-token");
        when(refreshTokenService.issue(eq(user.getId()), anyString(), anyString())).thenReturn("refresh-token");

        // ACT
        LoginResponse response = authService.login(request, "127.0.0.1", "agent");

        // ASSERT
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(user.isLocked()).isFalse();
        assertThat(user.getLockedUntil()).isNull();
    }

    @Test
    @DisplayName("login() — TOTP-enabled account is rejected (2FA not yet implemented)")
    void login_totpEnabled_throws() {
        // ARRANGE
        User user = buildUser();
        user.setTotpEnabled(true);
        when(userRepository.findByUsernameOrEmail("jdoe")).thenReturn(Optional.of(user));
        LoginRequest request = new LoginRequest();
        request.setIdentifier("jdoe");
        request.setPassword("correct-password");

        // ACT / ASSERT
        assertThatThrownBy(() -> authService.login(request, "127.0.0.1", "agent"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("TOTP");
    }

    @Test
    @DisplayName("refresh() — valid refresh token issues a new access token")
    void refresh_validToken_returnsNewAccessToken() {
        // ARRANGE
        User user = buildUser();
        RefreshToken token = RefreshToken.builder()
                .userId(user.getId())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(refreshTokenService.findValid("raw-token")).thenReturn(Optional.of(token));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessToken(user.getId(), "jdoe", "jdoe@college.edu"))
                .thenReturn("new-access-token");

        // ACT
        var response = authService.refresh("raw-token");

        // ASSERT
        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("raw-token");
    }

    @Test
    @DisplayName("refresh() — invalid or expired token is rejected")
    void refresh_invalidToken_throws() {
        // ARRANGE
        when(refreshTokenService.findValid("bad-token")).thenReturn(Optional.empty());

        // ACT / ASSERT
        assertThatThrownBy(() -> authService.refresh("bad-token"))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("changePassword() — correct current password updates the hash")
    void changePassword_success_updatesHash() {
        // ARRANGE
        User user = buildUser();
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("old-password");
        request.setNewPassword("new-password-123");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old-password", "hashed-password")).thenReturn(true);
        when(passwordEncoder.encode("new-password-123")).thenReturn("new-hashed-password");

        // ACT
        authService.changePassword(user.getId(), request);

        // ASSERT
        assertThat(user.getPasswordHash()).isEqualTo("new-hashed-password");
        assertThat(user.isMustChangePassword()).isFalse();
        verify(auditLogWriter).write(eq(AuditAction.PASSWORD_CHANGED), anyString(), eq("User"),
                eq(user.getId().toString()), anyString());
    }

    @Test
    @DisplayName("changePassword() — wrong current password is rejected, hash left unchanged")
    void changePassword_wrongCurrentPassword_throws() {
        // ARRANGE
        User user = buildUser();
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("not-the-current-password");
        request.setNewPassword("new-password-123");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("not-the-current-password", "hashed-password")).thenReturn(false);

        // ACT / ASSERT
        assertThatThrownBy(() -> authService.changePassword(user.getId(), request))
                .isInstanceOf(BusinessRuleViolationException.class);
        assertThat(user.getPasswordHash()).isEqualTo("hashed-password");
    }
}
