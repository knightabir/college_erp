package com.college.erp.module.finance.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** 05-api-documentation.md §7 POST /finance/fee-structures. */
@Getter
@Setter
public class CreateFeeStructureRequest {

    @NotNull(message = "Course is required")
    private UUID courseId;

    /** Null = applies to every batch of the course (03-srs.md [FIN-001]). */
    private UUID batchId;

    @NotNull(message = "Fee head is required")
    private UUID feeHeadId;

    @Min(value = 1, message = "Term number must be positive")
    private int termNumber;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    private Integer dueDateOffsetDays;
    private BigDecimal lateFeePerDay;
}
