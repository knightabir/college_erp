package com.college.erp.module.library.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.college.erp.module.library.domain.FineStatus;
import com.college.erp.module.library.domain.LibraryFine;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FineDto {

    private UUID id;
    private UUID userId;
    private UUID custodyLogId;
    private BigDecimal amount;
    private BigDecimal paidAmount;
    private BigDecimal balanceDue;
    private FineStatus status;
    private String waiverReason;

    public static FineDto from(LibraryFine fine) {
        return FineDto.builder()
                .id(fine.getId())
                .userId(fine.getUserId())
                .custodyLogId(fine.getCustodyLog().getId())
                .amount(fine.getAmount())
                .paidAmount(fine.getPaidAmount())
                .balanceDue(fine.balanceDue())
                .status(fine.getStatus())
                .waiverReason(fine.getWaiverReason())
                .build();
    }
}
