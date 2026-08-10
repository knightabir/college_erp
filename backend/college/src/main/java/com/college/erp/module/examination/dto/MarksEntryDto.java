package com.college.erp.module.examination.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.college.erp.module.examination.domain.MarksEntry;
import com.college.erp.module.examination.domain.MarksStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MarksEntryDto {

    private UUID id;
    private UUID examId;
    private UUID offeringId;
    private UUID studentId;
    private BigDecimal internalMarks;
    private BigDecimal externalMarks;
    private BigDecimal totalMarks;
    private Boolean pass;
    private String grade;
    private BigDecimal gradePoints;
    private MarksStatus status;

    public static MarksEntryDto from(MarksEntry entry) {
        return MarksEntryDto.builder()
                .id(entry.getId())
                .examId(entry.getExam().getId())
                .offeringId(entry.getOffering().getId())
                .studentId(entry.getStudent().getId())
                .internalMarks(entry.getInternalMarks())
                .externalMarks(entry.getExternalMarks())
                .totalMarks(entry.getTotalMarks())
                .pass(entry.getPass())
                .grade(entry.getGrade())
                .gradePoints(entry.getGradePoints())
                .status(entry.getStatus())
                .build();
    }
}
