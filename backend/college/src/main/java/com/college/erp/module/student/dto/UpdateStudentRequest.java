package com.college.erp.module.student.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** 05-api-documentation.md §4 PUT /students/{id} — deliberately narrower than CreateStudentRequest. */
@Getter
@Setter
public class UpdateStudentRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 100)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    private String lastName;

    private String phone;
    private String bloodGroup;
    private String address;
    private String city;
    private String state;
    private String pincode;
}
