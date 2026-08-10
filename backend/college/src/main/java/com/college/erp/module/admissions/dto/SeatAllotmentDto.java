package com.college.erp.module.admissions.dto;

import java.time.Instant;
import java.util.UUID;

import com.college.erp.module.admissions.domain.SeatAllotment;
import com.college.erp.module.admissions.domain.SeatAllotmentStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SeatAllotmentDto {

    private UUID id;
    private UUID applicationId;
    private String applicantName;
    private UUID courseId;
    private UUID batchId;
    private SeatAllotmentStatus status;
    private Instant offeredAt;
    private Instant responseDeadline;
    private Instant respondedAt;

    public static SeatAllotmentDto from(SeatAllotment allotment) {
        return SeatAllotmentDto.builder()
                .id(allotment.getId())
                .applicationId(allotment.getApplication().getId())
                .applicantName(allotment.getApplication().getApplicantName())
                .courseId(allotment.getCourse().getId())
                .batchId(allotment.getBatch() != null ? allotment.getBatch().getId() : null)
                .status(allotment.getStatus())
                .offeredAt(allotment.getOfferedAt())
                .responseDeadline(allotment.getResponseDeadline())
                .respondedAt(allotment.getRespondedAt())
                .build();
    }
}
