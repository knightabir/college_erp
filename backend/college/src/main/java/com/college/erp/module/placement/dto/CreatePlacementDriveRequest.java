package com.college.erp.module.placement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePlacementDriveRequest {

    @NotNull
    private UUID companyId;

    @NotNull
    private UUID academicYearId;

    @NotBlank
    private String title;

    private String description;
    private UUID eligibleCourseId;
    private BigDecimal eligibilityMinCgpa;
    private BigDecimal ctcOffered;
    private LocalDate driveDate;
}
