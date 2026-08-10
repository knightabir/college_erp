package com.college.erp.module.attendance.dto;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

/** A student below the course's minimum attendance threshold for a given offering (03-srs.md [ATT-005]). */
@Getter
@Builder
public class DefaulterDto {

    private UUID studentId;
    private String enrollmentNumber;
    private String studentName;
    private double attendancePercentage;
    private int minRequiredPct;
}
