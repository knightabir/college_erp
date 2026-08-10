package com.college.erp.module.hostel.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.college.erp.module.hostel.domain.RoomAllocation;
import com.college.erp.module.hostel.domain.RoomAllocationStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RoomAllocationDto {

    private UUID id;
    private UUID roomId;
    private String roomNumber;
    private UUID studentId;
    private String studentName;
    private LocalDate fromDate;
    private LocalDate toDate;
    private RoomAllocationStatus status;

    public static RoomAllocationDto from(RoomAllocation allocation) {
        return RoomAllocationDto.builder()
                .id(allocation.getId())
                .roomId(allocation.getRoom().getId())
                .roomNumber(allocation.getRoom().getRoomNumber())
                .studentId(allocation.getStudent().getId())
                .studentName(allocation.getStudent().getFullName())
                .fromDate(allocation.getFromDate())
                .toDate(allocation.getToDate())
                .status(allocation.getStatus())
                .build();
    }
}
