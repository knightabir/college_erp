package com.college.erp.module.admissions.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.college.erp.module.admissions.domain.ApplicationCategory;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** INVENTED — see V015's header comment. */
@Getter
@Setter
public class CreateApplicationRequest {

    @NotBlank
    private String applicantName;

    @NotBlank
    @Email
    private String applicantEmail;

    private String applicantPhone;
    private LocalDate dateOfBirth;
    private String gender;

    @NotNull
    private UUID courseId;

    @NotNull
    private UUID academicYearId;

    private ApplicationCategory category;
    private BigDecimal qualifyingExamScore;
    private String qualifyingExamDetails;
}
