package com.college.erp.module.admin.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.college.erp.module.auth.domain.User;

import lombok.Builder;
import lombok.Getter;

/** Deliberately excludes passwordHash/totpSecret — never leaves the service layer. */
@Getter
@Builder
public class UserDto {

    private UUID id;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private boolean enabled;
    private boolean locked;
    private Instant lastLoginAt;
    private boolean mustChangePassword;
    private List<String> roles;

    public static UserDto from(User user, List<String> roles) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .enabled(user.isEnabled())
                .locked(user.isLocked())
                .lastLoginAt(user.getLastLoginAt())
                .mustChangePassword(user.isMustChangePassword())
                .roles(roles)
                .build();
    }
}
