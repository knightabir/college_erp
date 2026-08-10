package com.college.erp.module.library.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.college.erp.module.library.domain.BorrowerType;
import com.college.erp.module.library.domain.CopyCondition;
import com.college.erp.module.library.domain.LibraryCustodyLog;

import lombok.Builder;
import lombok.Getter;

/** 05-api-documentation.md §8 — LibraryCustodyLog response shape / custody-chain entries. */
@Getter
@Builder
public class CustodyLogDto {

    private UUID id;
    private UUID copyId;
    private String borrowerName;
    private BorrowerType borrowerType;
    private Instant issuedAt;
    private Instant dueAt;
    private Instant returnedAt;
    private CopyCondition conditionOnIssue;
    private CopyCondition conditionOnReturn;
    private BigDecimal fineAmount;

    public static CustodyLogDto from(LibraryCustodyLog log) {
        return CustodyLogDto.builder()
                .id(log.getId())
                .copyId(log.getCopy().getId())
                .borrowerName(log.getBorrowerName())
                .borrowerType(log.getBorrowerType())
                .issuedAt(log.getIssuedAt())
                .dueAt(log.getDueAt())
                .returnedAt(log.getReturnedAt())
                .conditionOnIssue(log.getConditionOnIssue())
                .conditionOnReturn(log.getConditionOnReturn())
                .fineAmount(log.getFineAmount())
                .build();
    }
}
