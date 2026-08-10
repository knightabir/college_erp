package com.college.erp.module.examination.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/** 05-api-documentation.md §6 POST /exams/{examId}/hall-tickets/generate response shape. */
@Getter
@Builder
public class HallTicketGenerationResultDto {

    private int generated;
    private int ineligible;
    private List<IneligibleStudentDto> ineligibleStudents;
}
