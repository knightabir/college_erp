package com.college.erp.module.admin.dto;

import java.time.Instant;
import java.util.UUID;

import com.college.erp.module.auth.domain.UserRole;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** 05-api-documentation.md §13: POST /admin/users/{id}/roles. */
@Getter
@Setter
public class AssignRoleRequest {

    @NotNull
    private UserRole role;

    private UUID departmentId;

    /** NULL = permanent, matching RoleAssignment.expiresAt. */
    private Instant expiresAt;
}
