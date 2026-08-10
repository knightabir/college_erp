package com.college.erp.module.examination.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.college.erp.module.examination.domain.ExamType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** 05-api-documentation.md §6 POST /exams. */
@Getter
@Setter
public class CreateExamRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Exam type is required")
    private ExamType examType;

    @NotNull(message = "Batch is required")
    private UUID batchId;

    @NotNull(message = "Term is required")
    private UUID termId;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;
}
