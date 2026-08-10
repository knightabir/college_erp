package com.college.erp.module.finance.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.college.erp.module.finance.domain.EntryType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 05-api-documentation.md §7 POST /finance/ledger/manual-entry. The documented request body is
 * single-sided (one account head, one entry type) even though 03-srs.md [FIN-004] calls for
 * "paired entries" — see LedgerService javadoc for how that's reconciled.
 */
@Getter
@Setter
public class ManualLedgerEntryRequest {

    @NotNull(message = "Account head is required")
    private UUID accountHeadId;

    @NotNull(message = "Entry type is required")
    private EntryType entryType;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    private BigDecimal amount;

    @NotBlank(message = "Description is required")
    private String description;

    private String referenceType;
    private String referenceId;
}
