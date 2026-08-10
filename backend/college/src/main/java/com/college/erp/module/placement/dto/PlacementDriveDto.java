package com.college.erp.module.placement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.college.erp.module.placement.domain.DriveStatus;
import com.college.erp.module.placement.domain.PlacementDrive;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlacementDriveDto {

    private UUID id;
    private UUID companyId;
    private String companyName;
    private UUID academicYearId;
    private String title;
    private String description;
    private UUID eligibleCourseId;
    private BigDecimal eligibilityMinCgpa;
    private BigDecimal ctcOffered;
    private LocalDate driveDate;
    private DriveStatus status;

    public static PlacementDriveDto from(PlacementDrive drive) {
        return PlacementDriveDto.builder()
                .id(drive.getId())
                .companyId(drive.getCompany().getId())
                .companyName(drive.getCompany().getName())
                .academicYearId(drive.getAcademicYear().getId())
                .title(drive.getTitle())
                .description(drive.getDescription())
                .eligibleCourseId(drive.getEligibleCourse() != null ? drive.getEligibleCourse().getId() : null)
                .eligibilityMinCgpa(drive.getEligibilityMinCgpa())
                .ctcOffered(drive.getCtcOffered())
                .driveDate(drive.getDriveDate())
                .status(drive.getStatus())
                .build();
    }
}
