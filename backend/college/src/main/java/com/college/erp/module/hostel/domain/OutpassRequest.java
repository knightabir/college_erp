package com.college.erp.module.hostel.domain;

import java.time.Instant;
import java.util.UUID;

import com.college.erp.common.domain.BaseEntity;
import com.college.erp.module.student.domain.Student;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 04-database-schema.md §14.1. */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "outpass_requests")
public class OutpassRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "from_datetime", nullable = false)
    private Instant fromDatetime;

    @Column(name = "to_datetime", nullable = false)
    private Instant toDatetime;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "destination", length = 200)
    private String destination;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OutpassStatus status = OutpassStatus.PENDING;

    @Column(name = "approved_by")
    private UUID approvedBy;

    public void approve(UUID approverUserId) {
        this.status = OutpassStatus.APPROVED;
        this.approvedBy = approverUserId;
    }

    public void reject(UUID approverUserId) {
        this.status = OutpassStatus.REJECTED;
        this.approvedBy = approverUserId;
    }

    public void cancel() {
        this.status = OutpassStatus.CANCELLED;
    }
}
