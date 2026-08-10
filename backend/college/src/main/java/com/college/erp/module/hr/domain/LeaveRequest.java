package com.college.erp.module.hr.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.college.erp.common.domain.BaseEntity;

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

/**
 * 04-database-schema.md §13.3, 03-srs.md [HR-003]/[HR-004]. [HR-004] describes a conceptual
 * "HOD → HR Manager" approval chain, but the schema has only one reviewed_by/reviewed_at/
 * review_remarks slot (no separate HOD-review vs HR-Manager-review columns) and
 * 05-api-documentation.md §10 exposes exactly one approve/reject endpoint, open to any of
 * SUPER_ADMIN/HR_MANAGER/HOD/ADMIN — so this is modeled as single-stage approval by whichever of
 * those roles acts first, not a persisted two-step handshake. Same schema-constrained
 * simplification as asset disposal ([AST-004]) — see that module's CLAUDE.md notes.
 */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "leave_requests")
public class LeaveRequest extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    @Column(name = "to_date", nullable = false)
    private LocalDate toDate;

    @Column(name = "days_requested", nullable = false)
    private int daysRequested;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LeaveRequestStatus status = LeaveRequestStatus.PENDING;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "review_remarks", columnDefinition = "TEXT")
    private String reviewRemarks;

    public void approve(UUID reviewerUserId, String remarks) {
        this.status = LeaveRequestStatus.APPROVED;
        this.reviewedBy = reviewerUserId;
        this.reviewedAt = Instant.now();
        this.reviewRemarks = remarks;
    }

    public void reject(UUID reviewerUserId, String remarks) {
        this.status = LeaveRequestStatus.REJECTED;
        this.reviewedBy = reviewerUserId;
        this.reviewedAt = Instant.now();
        this.reviewRemarks = remarks;
    }

    public void cancel() {
        this.status = LeaveRequestStatus.CANCELLED;
    }
}
