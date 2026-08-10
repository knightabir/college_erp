package com.college.erp.module.finance.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** 05-api-documentation.md §7 POST /finance/accountant-grants. */
@Getter
@Setter
public class CreateAccountantGrantRequest {

    @NotNull(message = "Grantee is required")
    private UUID granteeUserId;

    @NotBlank(message = "Permission key is required")
    private String permissionKey;

    private UUID scopeDepartmentId;
    private UUID scopeBatchId;
    private String scopeFeeHeadCode;
    private BigDecimal amountThreshold;

    @NotNull(message = "Effective from is required")
    private Instant effectiveFrom;

    private Instant effectiveUntil;
}
