package com.college.erp.module.hr.dto;

import java.util.UUID;

import com.college.erp.module.hr.domain.LeaveType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LeaveTypeDto {

    private UUID id;
    private String code;
    private String name;
    private int daysPerYear;
    private boolean paid;

    public static LeaveTypeDto from(LeaveType leaveType) {
        return LeaveTypeDto.builder()
                .id(leaveType.getId())
                .code(leaveType.getCode())
                .name(leaveType.getName())
                .daysPerYear(leaveType.getDaysPerYear())
                .paid(leaveType.isPaid())
                .build();
    }
}
