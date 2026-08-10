package com.college.erp.module.attendance.dto;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

/** 05-api-documentation.md §5 GET /attendance/summary/student/{studentId}/offering/{offeringId}. */
@Getter
@Builder
public class AttendanceSummaryDto {

    private UUID studentId;
    private UUID offeringId;
    private int totalClasses;
    private int present;
    private int absent;
    private double attendancePercentage;
    private boolean eligibleForExam;
}
