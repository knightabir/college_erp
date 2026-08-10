package com.college.erp.module.academic.dto;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTermRequest {

    @NotNull(message = "Academic year is required")
    private UUID academicYearId;

    @NotBlank(message = "Name is required")
    private String name;

    @Min(value = 1, message = "Term number must be positive")
    private int termNumber;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    private LocalDate examStartDate;
    private LocalDate examEndDate;
}
