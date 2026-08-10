package com.college.erp.module.attendance.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.college.erp.module.attendance.domain.AttendanceSession;
import com.college.erp.module.attendance.domain.SessionStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AttendanceSessionDto {

    private UUID id;
    private UUID offeringId;
    private UUID sectionId;
    private UUID facultyUserId;
    private LocalDate sessionDate;
    private SessionStatus status;
    private String remarks;

    public static AttendanceSessionDto from(AttendanceSession session) {
        return AttendanceSessionDto.builder()
                .id(session.getId())
                .offeringId(session.getOffering().getId())
                .sectionId(session.getSection().getId())
                .facultyUserId(session.getFacultyUserId())
                .sessionDate(session.getSessionDate())
                .status(session.getStatus())
                .remarks(session.getRemarks())
                .build();
    }
}
