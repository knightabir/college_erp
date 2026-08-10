package com.college.erp.module.library.domain;

import java.math.BigDecimal;
import java.util.UUID;

import com.college.erp.common.domain.BaseEntity;

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

/** 04-database-schema.md §11.6, 03-srs.md [LIB-005]/[LIB-006]. */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "library_fines", indexes = @Index(name = "idx_fine_user", columnList = "user_id"))
public class LibraryFine extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "custody_log_id", nullable = false)
    private LibraryCustodyLog custodyLog;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Builder.Default
    @Column(name = "paid_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FineStatus status = FineStatus.UNPAID;

    @Column(name = "waived_by_user_id")
    private UUID waivedByUserId;

    @Column(name = "waiver_reason", columnDefinition = "TEXT")
    private String waiverReason;

    public BigDecimal balanceDue() {
        return amount.subtract(paidAmount);
    }
}
