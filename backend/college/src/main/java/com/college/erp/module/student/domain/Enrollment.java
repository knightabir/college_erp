package com.college.erp.module.student.domain;

import java.time.LocalDate;

import com.college.erp.common.domain.TenantScopedEntity;
import com.college.erp.module.academic.domain.Batch;
import com.college.erp.module.academic.domain.Section;
import com.college.erp.module.academic.domain.Term;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 04-database-schema.md §7.2 — versioned, not overwritten. Every section transfer or promotion
 * inserts a new row and flips the previous current row's `current` to false; existing rows are
 * never edited otherwise. Not physically trigger-enforced immutable like audit_log/ledger_entries
 * (§17 lists only those four) — this invariant is maintained by EnrollmentService, in one
 * transaction per change (§4.3 in 07-coding-standards.md — one service method, one transaction).
 */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "enrollments",
    indexes = {
        @Index(name = "idx_enrollment_student", columnList = "student_id"),
        @Index(name = "idx_enrollment_section", columnList = "section_id"),
        @Index(name = "idx_enrollment_current", columnList = "student_id, is_current")
    }
)
public class Enrollment extends TenantScopedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "term_id", nullable = false)
    private Term term;

    @Column(name = "enrollment_date", nullable = false)
    private LocalDate enrollmentDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EnrollmentStatus status = EnrollmentStatus.ACTIVE;

    @Builder.Default
    @Column(name = "is_current", nullable = false)
    private boolean current = true;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;
}
