package com.college.erp.module.finance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateVendorRequest {

    @NotBlank(message = "Code is required")
    private String code;

    @NotBlank(message = "Name is required")
    private String name;

    private String contactPerson;
    private String email;
    private String phone;
    private String address;
    private String gstNumber;
    private String panNumber;
    private String bankAccountNumber;
    private String ifscCode;
}
