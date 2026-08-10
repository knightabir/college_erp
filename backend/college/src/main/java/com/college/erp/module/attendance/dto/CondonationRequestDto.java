package com.college.erp.module.attendance.dto;

import java.util.UUID;

import com.college.erp.module.attendance.domain.CondonationRequest;
import com.college.erp.module.attendance.domain.CondonationStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CondonationRequestDto {

    private UUID id;
    private UUID studentId;
    private UUID offeringId;
    private UUID termId;
    private String reason;
    private CondonationStatus status;
    private UUID reviewedByUserId;
    private String reviewRemarks;

    public static CondonationRequestDto from(CondonationRequest request) {
        return CondonationRequestDto.builder()
                .id(request.getId())
                .studentId(request.getStudent().getId())
                .offeringId(request.getOffering().getId())
                .termId(request.getTerm().getId())
                .reason(request.getReason())
                .status(request.getStatus())
                .reviewedByUserId(request.getReviewedByUserId())
                .reviewRemarks(request.getReviewRemarks())
                .build();
    }
}
