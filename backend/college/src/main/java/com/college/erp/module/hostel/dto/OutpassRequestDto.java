package com.college.erp.module.hostel.dto;

import java.time.Instant;
import java.util.UUID;

import com.college.erp.module.hostel.domain.OutpassRequest;
import com.college.erp.module.hostel.domain.OutpassStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OutpassRequestDto {

    private UUID id;
    private UUID studentId;
    private String studentName;
    private Instant fromDatetime;
    private Instant toDatetime;
    private String reason;
    private String destination;
    private OutpassStatus status;
    private UUID approvedBy;

    public static OutpassRequestDto from(OutpassRequest request) {
        return OutpassRequestDto.builder()
                .id(request.getId())
                .studentId(request.getStudent().getId())
                .studentName(request.getStudent().getFullName())
                .fromDatetime(request.getFromDatetime())
                .toDatetime(request.getToDatetime())
                .reason(request.getReason())
                .destination(request.getDestination())
                .status(request.getStatus())
                .approvedBy(request.getApprovedBy())
                .build();
    }
}
