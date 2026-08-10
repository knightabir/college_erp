package com.college.erp.module.examination.dto;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;

/** One row of ResultDto.subjects — 05-api-documentation.md §6 GET .../results/student/.../term/... */
@Getter
@Builder
public class SubjectResultDto {

    private String subjectCode;
    private String subjectName;
    private BigDecimal internalMarks;
    private BigDecimal externalMarks;
    private BigDecimal total;
    private String grade;
    private BigDecimal gradePoints;
    private boolean pass;
}
