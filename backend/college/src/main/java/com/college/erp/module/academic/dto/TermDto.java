package com.college.erp.module.academic.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.college.erp.module.academic.domain.Term;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TermDto {

    private UUID id;
    private UUID academicYearId;
    private String name;
    private int termNumber;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate examStartDate;
    private LocalDate examEndDate;
    private boolean active;

    public static TermDto from(Term term) {
        return TermDto.builder()
                .id(term.getId())
                .academicYearId(term.getAcademicYear().getId())
                .name(term.getName())
                .termNumber(term.getTermNumber())
                .startDate(term.getStartDate())
                .endDate(term.getEndDate())
                .examStartDate(term.getExamStartDate())
                .examEndDate(term.getExamEndDate())
                .active(term.isActive())
                .build();
    }
}
