package com.college.erp.module.attendance.domain;

import com.college.erp.common.domain.BaseEntity;
import com.college.erp.module.academic.domain.Course;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 04-database-schema.md §8.1. One policy per course, at most. */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "attendance_policies")
public class AttendancePolicy extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false, unique = true)
    private Course course;

    @Builder.Default
    @Column(name = "min_attendance_pct", nullable = false)
    private int minAttendancePct = 75;

    @Builder.Default
    @Column(name = "medical_condonation_pct", nullable = false)
    private int medicalCondonationPct = 65;

    @Builder.Default
    @Column(name = "consecutive_absence_alert", nullable = false)
    private int consecutiveAbsenceAlert = 3;
}
