package com.college.erp.module.finance.dto;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** 05-api-documentation.md §7 POST /finance/fees/invoices/generate. */
@Getter
@Setter
public class GenerateInvoiceRequest {

    @NotNull(message = "Student is required")
    private UUID studentId;

    @NotNull(message = "Course is required")
    private UUID courseId;

    @NotNull(message = "Batch is required")
    private UUID batchId;

    @NotNull(message = "Academic year is required")
    private UUID academicYearId;

    @Min(value = 1, message = "Term number must be positive")
    private int termNumber;
}
