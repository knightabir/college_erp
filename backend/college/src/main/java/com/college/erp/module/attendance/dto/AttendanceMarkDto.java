package com.college.erp.module.attendance.dto;

import java.util.UUID;

import com.college.erp.module.attendance.domain.AttendanceRecord;
import com.college.erp.module.attendance.domain.AttendanceStatus;

import lombok.Builder;
import lombok.Getter;

/**
 * One student's mark within a session. NOT the request shape for POST .../mark — that request
 * body is a bare {student_id: status} JSON object per 05-api-documentation.md §5, taken directly
 * as Map&lt;UUID, AttendanceStatus&gt; in AttendanceCaptureService/the controller (a wrapper class
 * would need custom Jackson (de)serialization to accept a bare object as its whole body). This
 * DTO is the per-record view for listing a session's current marks back.
 */
@Getter
@Builder
public class AttendanceMarkDto {

    private UUID studentId;
    private AttendanceStatus status;
    private String remarks;

    public static AttendanceMarkDto from(AttendanceRecord record) {
        return AttendanceMarkDto.builder()
                .studentId(record.getStudent().getId())
                .status(record.getStatus())
                .remarks(record.getRemarks())
                .build();
    }
}
