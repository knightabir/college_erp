package com.college.erp.module.admin.dto;

import java.time.Instant;
import java.util.UUID;

import com.college.erp.module.auth.domain.RoleAssignment;
import com.college.erp.module.auth.domain.UserRole;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RoleAssignmentDto {

    private UUID id;
    private UUID userId;
    private UserRole role;
    private UUID departmentId;
    private UUID assignedBy;
    private Instant assignedAt;
    private Instant expiresAt;
    private boolean active;

    public static RoleAssignmentDto from(RoleAssignment assignment) {
        return RoleAssignmentDto.builder()
                .id(assignment.getId())
                .userId(assignment.getUserId())
                .role(assignment.getRole())
                .departmentId(assignment.getDepartmentId())
                .assignedBy(assignment.getAssignedBy())
                .assignedAt(assignment.getAssignedAt())
                .expiresAt(assignment.getExpiresAt())
                .active(assignment.isActive())
                .build();
    }
}
