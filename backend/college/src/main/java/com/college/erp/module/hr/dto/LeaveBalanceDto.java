package com.college.erp.module.hr.dto;

import java.util.UUID;

import com.college.erp.module.hr.domain.LeaveBalance;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LeaveBalanceDto {

    private UUID id;
    private UUID userId;
    private UUID leaveTypeId;
    private String leaveTypeName;
    private int year;
    private int totalDays;
    private int usedDays;
    private int remainingDays;

    public static LeaveBalanceDto from(LeaveBalance balance) {
        return LeaveBalanceDto.builder()
                .id(balance.getId())
                .userId(balance.getUserId())
                .leaveTypeId(balance.getLeaveType().getId())
                .leaveTypeName(balance.getLeaveType().getName())
                .year(balance.getYear())
                .totalDays(balance.getTotalDays())
                .usedDays(balance.getUsedDays())
                .remainingDays(balance.remainingDays())
                .build();
    }
}
