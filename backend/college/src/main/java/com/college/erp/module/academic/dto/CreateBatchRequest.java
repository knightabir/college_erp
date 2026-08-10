package com.college.erp.module.academic.dto;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateBatchRequest {

    @NotNull(message = "Course is required")
    private UUID courseId;

    @NotNull(message = "Academic year is required")
    private UUID academicYearId;

    @NotBlank(message = "Label is required")
    private String label;

    @Min(value = 1900, message = "Intake year must be valid")
    private int intakeYear;
}
