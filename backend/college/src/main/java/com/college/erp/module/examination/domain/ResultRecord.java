package com.college.erp.module.examination.domain;

import java.math.BigDecimal;
import java.time.Instant;

import com.college.erp.common.domain.BaseEntity;
import com.college.erp.module.academic.domain.Batch;
import com.college.erp.module.academic.domain.Term;
import com.college.erp.module.student.domain.Student;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

/** 04-database-schema.md §9.4, 03-srs.md [EXAM-005]. One row per (student, term) — recomputed
 *  in place each time an exam in that term is published, not re-inserted. */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "result_records", uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "term_id"}))
public class ResultRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "term_id", nullable = false)
    private Term term;

    @Column(name = "sgpa", precision = 4, scale = 2)
    private BigDecimal sgpa;

    @Column(name = "cgpa", precision = 4, scale = 2)
    private BigDecimal cgpa;

    @Column(name = "total_credits_earned")
    private Integer totalCreditsEarned;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_status", nullable = false, length = 20)
    private ResultStatus resultStatus;

    @Column(name = "published_at")
    private Instant publishedAt;
}
