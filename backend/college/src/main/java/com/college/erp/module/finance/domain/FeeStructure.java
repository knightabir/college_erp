package com.college.erp.module.finance.domain;

import java.math.BigDecimal;

import com.college.erp.common.domain.BaseEntity;
import com.college.erp.module.academic.domain.Batch;
import com.college.erp.module.academic.domain.Course;

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

/** 04-database-schema.md §10.4, 03-srs.md [FIN-001]. batch null = applies to every batch of the course. */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "fee_structures",
    uniqueConstraints = @UniqueConstraint(columnNames = {"course_id", "batch_id", "fee_head_id", "term_number"})
)
public class FeeStructure extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private Batch batch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fee_head_id", nullable = false)
    private FeeHead feeHead;

    @Column(name = "term_number", nullable = false)
    private int termNumber;

    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "due_date_offset_days")
    private Integer dueDateOffsetDays;

    @Column(name = "late_fee_per_day", precision = 10, scale = 2)
    private BigDecimal lateFeePerDay;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
