package com.college.erp.security.jwt;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.college.erp.config.JwtProperties;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-secret-key-must-be-at-least-256-bits-long-for-hs256!!");
        properties.setAccessExpiry(900_000L);
        jwtTokenProvider = new JwtTokenProvider(properties);
    }

    @Test
    @DisplayName("generateAccessToken() — round-trips id/username/email through validate + getters")
    void generateAccessToken_roundTrip() {
        // ARRANGE
        UUID userId = UUID.randomUUID();

        // ACT
        String token = jwtTokenProvider.generateAccessToken(userId, "jdoe", "jdoe@college.edu");

        // ASSERT
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUserId(token)).isEqualTo(userId);
        assertThat(jwtTokenProvider.getUsername(token)).isEqualTo("jdoe");
        assertThat(jwtTokenProvider.getEmail(token)).isEqualTo("jdoe@college.edu");
    }

    @Test
    @DisplayName("validateToken() — tampered token returns false")
    void validateToken_tamperedToken_returnsFalse() {
        // ARRANGE
        String token = jwtTokenProvider.generateAccessToken(UUID.randomUUID(), "jdoe", "jdoe@college.edu");
        String tampered = token.substring(0, token.length() - 4) + "abcd";

        // ACT / ASSERT
        assertThat(jwtTokenProvider.validateToken(tampered)).isFalse();
    }

    @Test
    @DisplayName("validateToken() — garbage input returns false, never throws")
    void validateToken_garbageInput_returnsFalse() {
        assertThat(jwtTokenProvider.validateToken("not-a-jwt")).isFalse();
    }

    @Test
    @DisplayName("validateToken() — expired token returns false")
    void validateToken_expiredToken_returnsFalse() {
        // ARRANGE — a provider configured to issue already-expired tokens
        JwtProperties expiredProperties = new JwtProperties();
        expiredProperties.setSecret("test-secret-key-must-be-at-least-256-bits-long-for-hs256!!");
        expiredProperties.setAccessExpiry(-1_000L);
        JwtTokenProvider expiredTokenProvider = new JwtTokenProvider(expiredProperties);

        // ACT
        String token = expiredTokenProvider.generateAccessToken(UUID.randomUUID(), "jdoe", "jdoe@college.edu");

        // ASSERT
        assertThat(expiredTokenProvider.validateToken(token)).isFalse();
    }
}
