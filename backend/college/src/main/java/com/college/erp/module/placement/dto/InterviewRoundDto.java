package com.college.erp.module.placement.dto;

import java.time.Instant;
import java.util.UUID;

import com.college.erp.module.placement.domain.InterviewResult;
import com.college.erp.module.placement.domain.InterviewRound;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InterviewRoundDto {

    private UUID id;
    private UUID driveApplicationId;
    private int roundNumber;
    private String roundType;
    private Instant scheduledAt;
    private InterviewResult result;
    private String feedback;
    private String interviewerName;

    public static InterviewRoundDto from(InterviewRound round) {
        return InterviewRoundDto.builder()
                .id(round.getId())
                .driveApplicationId(round.getDriveApplication().getId())
                .roundNumber(round.getRoundNumber())
                .roundType(round.getRoundType())
                .scheduledAt(round.getScheduledAt())
                .result(round.getResult())
                .feedback(round.getFeedback())
                .interviewerName(round.getInterviewerName())
                .build();
    }
}
