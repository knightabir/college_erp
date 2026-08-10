package com.college.erp.module.hr.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.college.erp.module.hr.domain.LeaveRequest;
import com.college.erp.module.hr.domain.LeaveRequestStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LeaveRequestDto {

    private UUID id;
    private UUID userId;
    private UUID leaveTypeId;
    private String leaveTypeName;
    private LocalDate fromDate;
    private LocalDate toDate;
    private int daysRequested;
    private String reason;
    private LeaveRequestStatus status;
    private UUID reviewedBy;
    private Instant reviewedAt;
    private String reviewRemarks;

    public static LeaveRequestDto from(LeaveRequest request) {
        return LeaveRequestDto.builder()
                .id(request.getId())
                .userId(request.getUserId())
                .leaveTypeId(request.getLeaveType().getId())
                .leaveTypeName(request.getLeaveType().getName())
                .fromDate(request.getFromDate())
                .toDate(request.getToDate())
                .daysRequested(request.getDaysRequested())
                .reason(request.getReason())
                .status(request.getStatus())
                .reviewedBy(request.getReviewedBy())
                .reviewedAt(request.getReviewedAt())
                .reviewRemarks(request.getReviewRemarks())
                .build();
    }
}
