package com.college.erp.module.academic.dto;

import java.util.UUID;

import com.college.erp.module.academic.domain.Department;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DepartmentDto {

    private UUID id;
    private String name;
    private String code;
    private String description;
    private UUID hodUserId;
    private String contactEmail;
    private String contactPhone;
    private boolean active;

    public static DepartmentDto from(Department department) {
        return DepartmentDto.builder()
                .id(department.getId())
                .name(department.getName())
                .code(department.getCode())
                .description(department.getDescription())
                .hodUserId(department.getHodUserId())
                .contactEmail(department.getContactEmail())
                .contactPhone(department.getContactPhone())
                .active(department.isActive())
                .build();
    }
}
