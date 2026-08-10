package com.college.erp.module.academic.dto;

import java.util.UUID;

import com.college.erp.module.academic.domain.SubjectType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateSubjectRequest {

    @NotBlank(message = "Code is required")
    @Size(max = 20)
    @Pattern(regexp = "^[A-Z0-9]+$", message = "Code must be alphanumeric uppercase")
    private String code;

    @NotBlank(message = "Name is required")
    @Size(max = 200)
    private String name;

    @NotNull(message = "Department is required")
    private UUID departmentId;

    private int credits;

    @NotNull(message = "Type is required")
    private SubjectType type;

    private String description;
}
