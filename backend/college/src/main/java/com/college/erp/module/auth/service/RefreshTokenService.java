package com.college.erp.module.auth.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.config.JwtProperties;
import com.college.erp.module.auth.domain.RefreshToken;
import com.college.erp.module.auth.domain.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;

/**
 * FR-AUTH-6: only the SHA-256 hash of a refresh token is ever persisted — the raw 64-byte random
 * value is returned to the client once and never stored (08-system-architecture.md §4.1).
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 64;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    /** Returns the raw token — give this to the client, never persist it as-is. */
    @Transactional
    public String issue(UUID userId, String deviceInfo, String ipAddress) {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshToken entity = RefreshToken.builder()
                .userId(userId)
                .tokenHash(hash(rawToken))
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .expiresAt(Instant.now().plusMillis(jwtProperties.getRefreshExpiry()))
                .build();
        refreshTokenRepository.save(entity);

        return rawToken;
    }

    @Transactional(readOnly = true)
    public Optional<RefreshToken> findValid(String rawToken) {
        return refreshTokenRepository.findByTokenHash(hash(rawToken))
                .filter(RefreshToken::isValid);
    }

    @Transactional
    public void revoke(RefreshToken token) {
        token.setRevokedAt(Instant.now());
        refreshTokenRepository.save(token);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException ex) {
            // SHA-256 is guaranteed available on every JVM (JLS-mandated algorithm) — unreachable.
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
