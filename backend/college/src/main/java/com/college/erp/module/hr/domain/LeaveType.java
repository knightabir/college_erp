package com.college.erp.module.hr.domain;

import com.college.erp.common.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 04-database-schema.md §13.3, 03-srs.md [HR-002]. */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "leave_types")
public class LeaveType extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true, length = 10)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "days_per_year", nullable = false)
    private int daysPerYear;

    @Builder.Default
    @Column(name = "is_paid", nullable = false)
    private boolean paid = true;
}
