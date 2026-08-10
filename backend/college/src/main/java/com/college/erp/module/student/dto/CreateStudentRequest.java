package com.college.erp.module.student.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.college.erp.module.student.domain.Gender;
import com.college.erp.module.student.domain.StudentCategory;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** 05-api-documentation.md §4 POST /students. */
@Getter
@Setter
public class CreateStudentRequest {

    private UUID userId;

    @NotBlank(message = "Enrollment number is required")
    @Size(max = 30)
    @Pattern(regexp = "^[A-Z0-9]+$", message = "Enrollment number must be alphanumeric uppercase")
    private String enrollmentNumber;

    @NotBlank(message = "First name is required")
    @Size(max = 100)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    private String lastName;

    @NotBlank
    @Email(message = "Must be a valid email address")
    private String personalEmail;

    private String phone;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotNull(message = "Gender is required")
    private Gender gender;

    private String bloodGroup;

    @Pattern(regexp = "^\\d{12}$", message = "Aadhar number must be 12 digits")
    private String aadharNumber;

    private StudentCategory category;

    private String address;
    private String city;
    private String state;
    private String pincode;

    @NotNull(message = "Batch is required")
    private UUID batchId;

    @NotNull(message = "Section is required")
    private UUID sectionId;
}
