package com.college.erp.module.finance.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.college.erp.module.finance.domain.FeeStructure;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FeeStructureDto {

    private UUID id;
    private UUID courseId;
    private UUID batchId;
    private UUID feeHeadId;
    private String feeHeadName;
    private int termNumber;
    private BigDecimal amount;
    private Integer dueDateOffsetDays;
    private BigDecimal lateFeePerDay;
    private boolean active;

    public static FeeStructureDto from(FeeStructure structure) {
        return FeeStructureDto.builder()
                .id(structure.getId())
                .courseId(structure.getCourse().getId())
                .batchId(structure.getBatch() == null ? null : structure.getBatch().getId())
                .feeHeadId(structure.getFeeHead().getId())
                .feeHeadName(structure.getFeeHead().getName())
                .termNumber(structure.getTermNumber())
                .amount(structure.getAmount())
                .dueDateOffsetDays(structure.getDueDateOffsetDays())
                .lateFeePerDay(structure.getLateFeePerDay())
                .active(structure.isActive())
                .build();
    }
}
