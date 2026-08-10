package com.college.erp.module.examination.dto;

import java.time.Instant;
import java.util.UUID;

import com.college.erp.module.examination.domain.HallTicket;

import lombok.Builder;
import lombok.Getter;

/** No "seating info" field — see HallTicket javadoc for why. */
@Getter
@Builder
public class HallTicketDto {

    private UUID id;
    private UUID examId;
    private UUID studentId;
    private String hallTicketNumber;
    private boolean eligible;
    private String ineligibilityReason;
    private Instant issuedAt;

    public static HallTicketDto from(HallTicket hallTicket) {
        return HallTicketDto.builder()
                .id(hallTicket.getId())
                .examId(hallTicket.getExam().getId())
                .studentId(hallTicket.getStudent().getId())
                .hallTicketNumber(hallTicket.getHallTicketNumber())
                .eligible(hallTicket.isEligible())
                .ineligibilityReason(hallTicket.getIneligibilityReason())
                .issuedAt(hallTicket.getIssuedAt())
                .build();
    }
}
