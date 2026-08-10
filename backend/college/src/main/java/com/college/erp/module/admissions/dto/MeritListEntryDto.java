package com.college.erp.module.admissions.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.college.erp.module.admissions.domain.MeritListEntry;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MeritListEntryDto {

    private UUID id;
    private UUID applicationId;
    private String applicantName;
    private int rank;
    private BigDecimal score;

    public static MeritListEntryDto from(MeritListEntry entry) {
        return MeritListEntryDto.builder()
                .id(entry.getId())
                .applicationId(entry.getApplication().getId())
                .applicantName(entry.getApplication().getApplicantName())
                .rank(entry.getRank())
                .score(entry.getScore())
                .build();
    }
}
