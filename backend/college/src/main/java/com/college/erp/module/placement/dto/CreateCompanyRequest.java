package com.college.erp.module.placement.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCompanyRequest {

    @NotBlank
    private String name;

    private String industry;
    private String website;
    private String contactPerson;
    private String contactEmail;
    private String contactPhone;
}
