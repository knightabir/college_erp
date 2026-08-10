package com.college.erp.module.academic.dto;

import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateDepartmentRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 150)
    private String name;

    @NotBlank(message = "Code is required")
    @Size(max = 20)
    @Pattern(regexp = "^[A-Z0-9]+$", message = "Code must be alphanumeric uppercase")
    private String code;

    private String description;

    private UUID hodUserId;

    @Email(message = "Must be a valid email address")
    private String contactEmail;

    private String contactPhone;
}
