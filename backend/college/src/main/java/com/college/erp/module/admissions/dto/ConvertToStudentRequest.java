package com.college.erp.module.admissions.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** EnrollmentConversionService: turns an ACCEPTED seat allotment into a real Student + initial
 *  Enrollment (via module.student.service.StudentService). Section allocation is a separate
 *  operational step from admissions itself, so the caller supplies it explicitly here — see
 *  EnrollmentConversionService's javadoc. */
@Getter
@Setter
public class ConvertToStudentRequest {

    @NotBlank
    private String enrollmentNumber;

    @NotNull
    private UUID sectionId;

    /** Optional — a login account may be created separately via module.admin and linked later. */
    private UUID userId;
}
