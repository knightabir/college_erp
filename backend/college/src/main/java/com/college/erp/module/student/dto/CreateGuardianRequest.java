package com.college.erp.module.student.dto;

import com.college.erp.module.student.domain.GuardianRelationship;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateGuardianRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Relationship is required")
    private GuardianRelationship relationship;

    @NotBlank(message = "Phone is required")
    private String phone;

    @Email(message = "Must be a valid email address")
    private String email;

    private String occupation;
    private Long annualIncome;
    private boolean primary;
}
