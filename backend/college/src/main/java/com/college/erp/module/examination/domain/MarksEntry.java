package com.college.erp.module.examination.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.college.erp.common.domain.BaseEntity;
import com.college.erp.module.academic.domain.SubjectOffering;
import com.college.erp.module.student.domain.Student;

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

/** 04-database-schema.md §9.3, 03-srs.md [EXAM-003]/[EXAM-004]. */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "marks_entries",
    indexes = {
        @Index(name = "idx_marks_student", columnList = "student_id"),
        @Index(name = "idx_marks_exam", columnList = "exam_id")
    },
    uniqueConstraints = @UniqueConstraint(columnNames = {"exam_id", "offering_id", "student_id"})
)
public class MarksEntry extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offering_id", nullable = false)
    private SubjectOffering offering;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "internal_marks", precision = 6, scale = 2)
    private BigDecimal internalMarks;

    @Column(name = "external_marks", precision = 6, scale = 2)
    private BigDecimal externalMarks;

    @Column(name = "total_marks", precision = 6, scale = 2)
    private BigDecimal totalMarks;

    @Column(name = "is_pass")
    private Boolean pass;

    @Column(name = "grade", length = 5)
    private String grade;

    @Column(name = "grade_points", precision = 4, scale = 2)
    private BigDecimal gradePoints;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MarksStatus status = MarksStatus.DRAFT;

    @Column(name = "entered_by")
    private UUID enteredBy;

    @Column(name = "moderated_by")
    private UUID moderatedBy;

    @Column(name = "moderated_at")
    private Instant moderatedAt;
}
