package com.college.erp.module.hr.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateLeaveTypeRequest {

    @NotBlank
    private String code;

    @NotBlank
    private String name;

    @Min(0)
    private int daysPerYear;

    private boolean paid = true;
}
