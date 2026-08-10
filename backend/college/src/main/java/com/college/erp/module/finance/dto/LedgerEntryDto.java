package com.college.erp.module.finance.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.college.erp.module.finance.domain.EntryType;
import com.college.erp.module.finance.domain.LedgerEntry;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LedgerEntryDto {

    private UUID id;
    private UUID accountHeadId;
    private String accountHeadCode;
    private String description;
    private EntryType entryType;
    private BigDecimal amount;
    private LocalDate entryDate;
    private UUID financialYearId;
    private String referenceType;
    private String referenceId;
    private Instant createdAt;

    public static LedgerEntryDto from(LedgerEntry entry) {
        return LedgerEntryDto.builder()
                .id(entry.getId())
                .accountHeadId(entry.getAccountHead().getId())
                .accountHeadCode(entry.getAccountHead().getCode())
                .description(entry.getDescription())
                .entryType(entry.getEntryType())
                .amount(entry.getAmount())
                .entryDate(entry.getEntryDate())
                .financialYearId(entry.getFinancialYear().getId())
                .referenceType(entry.getReferenceType())
                .referenceId(entry.getReferenceId())
                .createdAt(entry.getCreatedAt())
                .build();
    }
}
