package com.college.erp.module.academic.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateSectionRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 10)
    private String name;

    @Min(value = 1, message = "Capacity must be positive")
    private int capacity;
}
