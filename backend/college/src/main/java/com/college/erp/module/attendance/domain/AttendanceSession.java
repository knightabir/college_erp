package com.college.erp.module.attendance.domain;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import com.college.erp.common.domain.BaseEntity;
import com.college.erp.module.academic.domain.Section;
import com.college.erp.module.academic.domain.SubjectOffering;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 04-database-schema.md §8.2, 03-srs.md [ATT-001]. */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "attendance_sessions",
    indexes = {
        @Index(name = "idx_att_session_offering", columnList = "offering_id"),
        @Index(name = "idx_att_session_section", columnList = "section_id"),
        @Index(name = "idx_att_session_date", columnList = "session_date")
    },
    uniqueConstraints = @UniqueConstraint(columnNames = {"offering_id", "section_id", "session_date"})
)
public class AttendanceSession extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offering_id", nullable = false)
    private SubjectOffering offering;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @Column(name = "faculty_user_id", nullable = false)
    private UUID facultyUserId;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SessionStatus status = SessionStatus.DRAFT;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;
}
