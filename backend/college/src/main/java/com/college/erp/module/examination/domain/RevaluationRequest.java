package com.college.erp.module.examination.domain;

import java.math.BigDecimal;
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

/**
 * 04-database-schema.md §9.5, 03-srs.md [EXAM-007]. fee_payment_id is left unset — revaluation fee
 * invoicing needs module.finance, which doesn't exist yet (the column is nullable in the doc's own
 * schema — "optional: revaluation fee" — so this isn't a workaround, just not wired up yet).
 */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "revaluation_requests")
public class RevaluationRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "marks_entry_id", nullable = false)
    private MarksEntry marksEntry;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "fee_payment_id")
    private UUID feePaymentId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RevaluationStatus status = RevaluationStatus.PENDING;

    @Column(name = "reviewed_marks", precision = 6, scale = 2)
    private BigDecimal reviewedMarks;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;
}
