package com.college.erp.module.examination.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** 05-api-documentation.md §6 POST /exams/marks. */
@Getter
@Setter
public class EnterMarksRequest {

    @NotNull(message = "Exam is required")
    private UUID examId;

    @NotNull(message = "Subject offering is required")
    private UUID offeringId;

    @NotNull(message = "Student is required")
    private UUID studentId;

    @DecimalMin(value = "0", message = "Internal marks cannot be negative")
    private BigDecimal internalMarks;

    @DecimalMin(value = "0", message = "External marks cannot be negative")
    private BigDecimal externalMarks;
}
