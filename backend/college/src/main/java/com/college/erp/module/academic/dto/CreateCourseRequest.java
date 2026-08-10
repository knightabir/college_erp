package com.college.erp.module.academic.dto;

import java.util.UUID;

import com.college.erp.module.academic.domain.CourseLevel;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCourseRequest {

    @NotNull(message = "Department is required")
    private UUID departmentId;

    @NotBlank(message = "Name is required")
    @Size(max = 200)
    private String name;

    @NotBlank(message = "Code is required")
    @Size(max = 20)
    @Pattern(regexp = "^[A-Z0-9]+$", message = "Code must be alphanumeric uppercase")
    private String code;

    @NotNull(message = "Level is required")
    private CourseLevel level;

    @Min(value = 1, message = "Duration must be at least 1 year")
    private int durationYears;

    private int totalCredits;

    @Min(value = 1, message = "Intake capacity must be positive")
    private int intakeCapacity;

    private String description;
}
