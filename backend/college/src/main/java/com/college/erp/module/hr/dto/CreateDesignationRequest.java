package com.college.erp.module.hr.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateDesignationRequest {

    @NotBlank
    private String name;

    private String grade;
}
