package com.college.erp.module.admin.dto;

import com.college.erp.module.auth.domain.UserRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** 05-api-documentation.md §13: POST /admin/users. */
@Getter
@Setter
public class CreateUserRequest {

    @NotBlank
    private String username;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String fullName;

    @NotBlank
    @Size(min = 8)
    private String password;

    private String phone;

    @NotNull
    private UserRole role;
}
