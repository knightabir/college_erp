package com.college.erp.module.academic.dto;

import java.util.UUID;

import com.college.erp.module.academic.domain.AcademicYear;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AcademicYearDto {

    private UUID id;
    private String label;
    private int startYear;
    private int endYear;
    private boolean current;

    public static AcademicYearDto from(AcademicYear year) {
        return AcademicYearDto.builder()
                .id(year.getId())
                .label(year.getLabel())
                .startYear(year.getStartYear())
                .endYear(year.getEndYear())
                .current(year.isCurrent())
                .build();
    }
}
