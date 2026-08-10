package com.college.erp.module.admissions.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.college.erp.module.admissions.domain.LateralEntryApplication;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LateralEntryApplicationDto {

    private UUID id;
    private UUID applicationId;
    private String diplomaInstitution;
    private BigDecimal diplomaScore;
    private Integer entryYearNumber;

    public static LateralEntryApplicationDto from(LateralEntryApplication entry) {
        return LateralEntryApplicationDto.builder()
                .id(entry.getId())
                .applicationId(entry.getApplication().getId())
                .diplomaInstitution(entry.getDiplomaInstitution())
                .diplomaScore(entry.getDiplomaScore())
                .entryYearNumber(entry.getEntryYearNumber())
                .build();
    }
}
