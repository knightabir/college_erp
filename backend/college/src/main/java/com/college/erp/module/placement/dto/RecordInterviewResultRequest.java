package com.college.erp.module.placement.dto;

import com.college.erp.module.placement.domain.InterviewResult;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecordInterviewResultRequest {

    @NotNull
    private InterviewResult result;

    private String feedback;
}
