package com.college.erp.module.academic.domain;

import com.college.erp.common.domain.TenantScopedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/** 04-database-schema.md §6.5. */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "batches",
    indexes = @Index(name = "idx_batch_course", columnList = "course_id"),
    uniqueConstraints = @UniqueConstraint(columnNames = {"course_id", "academic_year_id"})
)
public class Batch extends TenantScopedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @Column(name = "label", nullable = false, length = 100)
    private String label;

    @Column(name = "intake_year", nullable = false)
    private int intakeYear;

    @Builder.Default
    @Column(name = "current_term_number", nullable = false)
    private int currentTermNumber = 1;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public void promoteTerm() {
        this.currentTermNumber += 1;
    }
}
