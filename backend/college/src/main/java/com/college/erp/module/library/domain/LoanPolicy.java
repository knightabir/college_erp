package com.college.erp.module.library.domain;

import java.math.BigDecimal;

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

/** 04-database-schema.md §11.1, 03-srs.md [LIB-004]/[LIB-005]/[LIB-008]. One policy per borrower type. */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "loan_policies")
public class LoanPolicy extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "borrower_type", nullable = false, unique = true, length = 30)
    private BorrowerType borrowerType;

    @Builder.Default
    @Column(name = "max_books", nullable = false)
    private int maxBooks = 3;

    @Builder.Default
    @Column(name = "loan_days", nullable = false)
    private int loanDays = 14;

    @Builder.Default
    @Column(name = "renewals_allowed", nullable = false)
    private int renewalsAllowed = 2;

    @Builder.Default
    @Column(name = "fine_per_day", nullable = false, precision = 8, scale = 2)
    private BigDecimal finePerDay = BigDecimal.valueOf(2.00);

    @Builder.Default
    @Column(name = "max_fine", precision = 10, scale = 2)
    private BigDecimal maxFine = BigDecimal.valueOf(200.00);
}
