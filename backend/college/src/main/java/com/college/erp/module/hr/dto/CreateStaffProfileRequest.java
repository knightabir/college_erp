package com.college.erp.module.hr.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.college.erp.module.hr.domain.StaffType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** 05-api-documentation.md §10 doesn't document a create-staff-profile endpoint explicitly, but
 *  [HR-001] requires one to exist for staff to have anything to apply leave against. */
@Getter
@Setter
public class CreateStaffProfileRequest {

    @NotNull
    private UUID userId;

    @NotBlank
    private String employeeNumber;

    @NotNull
    private UUID departmentId;

    private UUID designationId;

    @NotNull
    private StaffType staffType;

    @NotNull
    private LocalDate joiningDate;

    private String qualification;
    private String specialization;
    private Integer experienceYears;
    private String bankAccountNumber;
    private String ifscCode;
    private String panNumber;
    private String pfNumber;
}
