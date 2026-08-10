package com.college.erp.module.examination.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.college.erp.module.examination.domain.ResultStatus;

import lombok.Builder;
import lombok.Getter;

/** 05-api-documentation.md §6 GET /exams/results/student/{studentId}/term/{termId}. Doubles as
 *  the data backing a grade card — see GradeCardService javadoc for why there's no PDF here. */
@Getter
@Builder
public class ResultDto {

    private UUID studentId;
    private UUID termId;
    private BigDecimal sgpa;
    private BigDecimal cgpa;
    private Integer totalCreditsEarned;
    private ResultStatus resultStatus;
    private List<SubjectResultDto> subjects;
}
