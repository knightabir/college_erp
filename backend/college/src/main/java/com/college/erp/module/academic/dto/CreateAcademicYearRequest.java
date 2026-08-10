package com.college.erp.module.academic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateAcademicYearRequest {

    @NotBlank(message = "Label is required")
    @Pattern(regexp = "^\\d{4}-\\d{4}$", message = "Label must look like '2024-2025'")
    private String label;

    private int startYear;
    private int endYear;
}
