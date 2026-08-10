package com.college.erp.module.finance.domain;

import java.time.LocalDate;
import java.util.UUID;

import com.college.erp.common.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 04-database-schema.md §10.1, 03-srs.md [FIN-009]. */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "financial_years")
public class FinancialYear extends BaseEntity {

    @Column(name = "label", nullable = false, unique = true, length = 20)
    private String label;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FinancialYearStatus status = FinancialYearStatus.OPEN;

    @Column(name = "closed_by_user_id")
    private UUID closedByUserId;

    @Column(name = "approved_by_user_id")
    private UUID approvedByUserId;
}
