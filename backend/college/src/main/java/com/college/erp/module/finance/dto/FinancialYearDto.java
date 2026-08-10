package com.college.erp.module.finance.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.college.erp.module.finance.domain.FinancialYear;
import com.college.erp.module.finance.domain.FinancialYearStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FinancialYearDto {

    private UUID id;
    private String label;
    private LocalDate startDate;
    private LocalDate endDate;
    private FinancialYearStatus status;
    private UUID closedByUserId;
    private UUID approvedByUserId;

    public static FinancialYearDto from(FinancialYear year) {
        return FinancialYearDto.builder()
                .id(year.getId())
                .label(year.getLabel())
                .startDate(year.getStartDate())
                .endDate(year.getEndDate())
                .status(year.getStatus())
                .closedByUserId(year.getClosedByUserId())
                .approvedByUserId(year.getApprovedByUserId())
                .build();
    }
}
