package com.college.erp.module.auth.dto;

import java.util.Set;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {

    private final UUID userId;
    private final String username;
    private final String fullName;
    private final String email;
    private final Set<String> roles;
    private final String accessToken;
    private final String refreshToken;
    private final long expiresIn; // seconds
}
