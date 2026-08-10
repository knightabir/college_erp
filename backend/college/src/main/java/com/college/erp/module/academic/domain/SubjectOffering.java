package com.college.erp.module.academic.domain;

import com.college.erp.common.domain.TenantScopedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 04-database-schema.md §6.8 — "this subject is taught to this batch in this term". */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "subject_offerings",
    uniqueConstraints = @UniqueConstraint(columnNames = {"batch_id", "subject_id", "term_id"})
)
public class SubjectOffering extends TenantScopedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "term_id", nullable = false)
    private Term term;

    @Builder.Default
    @Column(name = "max_internal_marks", nullable = false)
    private int maxInternalMarks = 40;

    @Builder.Default
    @Column(name = "max_external_marks", nullable = false)
    private int maxExternalMarks = 60;

    @Builder.Default
    @Column(name = "pass_marks_internal", nullable = false)
    private int passMarksInternal = 16;

    @Builder.Default
    @Column(name = "pass_marks_external", nullable = false)
    private int passMarksExternal = 24;

    @Builder.Default
    @Column(name = "is_elective", nullable = false)
    private boolean elective = false;
}
