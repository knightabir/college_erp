package com.college.erp.module.examination.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.college.erp.module.examination.domain.Exam;
import com.college.erp.module.examination.domain.ExamStatus;
import com.college.erp.module.examination.domain.ExamType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExamDto {

    private UUID id;
    private String name;
    private ExamType examType;
    private UUID batchId;
    private UUID termId;
    private LocalDate startDate;
    private LocalDate endDate;
    private ExamStatus status;

    public static ExamDto from(Exam exam) {
        return ExamDto.builder()
                .id(exam.getId())
                .name(exam.getName())
                .examType(exam.getExamType())
                .batchId(exam.getBatch().getId())
                .termId(exam.getTerm().getId())
                .startDate(exam.getStartDate())
                .endDate(exam.getEndDate())
                .status(exam.getStatus())
                .build();
    }
}
