package com.college.erp.module.examination.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.college.erp.module.examination.domain.RevaluationRequest;
import com.college.erp.module.examination.domain.RevaluationStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RevaluationRequestDto {

    private UUID id;
    private UUID marksEntryId;
    private UUID studentId;
    private String reason;
    private RevaluationStatus status;
    private BigDecimal reviewedMarks;
    private UUID reviewedBy;
    private Instant reviewedAt;

    public static RevaluationRequestDto from(RevaluationRequest request) {
        return RevaluationRequestDto.builder()
                .id(request.getId())
                .marksEntryId(request.getMarksEntry().getId())
                .studentId(request.getStudent().getId())
                .reason(request.getReason())
                .status(request.getStatus())
                .reviewedMarks(request.getReviewedMarks())
                .reviewedBy(request.getReviewedBy())
                .reviewedAt(request.getReviewedAt())
                .build();
    }
}
