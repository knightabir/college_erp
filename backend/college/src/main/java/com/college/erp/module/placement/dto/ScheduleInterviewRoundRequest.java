package com.college.erp.module.placement.dto;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScheduleInterviewRoundRequest {

    private String roundType;
    private Instant scheduledAt;
    private String interviewerName;
}
