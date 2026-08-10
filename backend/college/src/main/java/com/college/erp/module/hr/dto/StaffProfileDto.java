package com.college.erp.module.hr.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.college.erp.module.hr.domain.StaffProfile;
import com.college.erp.module.hr.domain.StaffType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StaffProfileDto {

    private UUID id;
    private UUID userId;
    private String employeeNumber;
    private UUID departmentId;
    private String departmentName;
    private UUID designationId;
    private String designationName;
    private StaffType staffType;
    private LocalDate joiningDate;
    private String qualification;
    private String specialization;
    private Integer experienceYears;

    public static StaffProfileDto from(StaffProfile profile) {
        return StaffProfileDto.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .employeeNumber(profile.getEmployeeNumber())
                .departmentId(profile.getDepartment().getId())
                .departmentName(profile.getDepartment().getName())
                .designationId(profile.getDesignation() != null ? profile.getDesignation().getId() : null)
                .designationName(profile.getDesignation() != null ? profile.getDesignation().getName() : null)
                .staffType(profile.getStaffType())
                .joiningDate(profile.getJoiningDate())
                .qualification(profile.getQualification())
                .specialization(profile.getSpecialization())
                .experienceYears(profile.getExperienceYears())
                .build();
    }
}
