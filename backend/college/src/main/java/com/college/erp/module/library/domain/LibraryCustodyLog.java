package com.college.erp.module.library.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 04-database-schema.md §11.4, 03-srs.md [LIB-003]. Partially immutable, mirroring
 * trg_custody_partial_immutable at the Java level too: no blanket @Setter — core issue fields
 * (copy, borrower*, issuedAt, issuedByUserId) can only ever be set via the constructor/builder;
 * the return fields are only ever touched through recordReturn(), the one blessed path, matching
 * what the DB trigger actually allows.
 */
@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "library_custody_log",
    indexes = {
        @Index(name = "idx_custody_copy", columnList = "copy_id"),
        @Index(name = "idx_custody_borrower", columnList = "borrower_user_id")
    }
)
public class LibraryCustodyLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "copy_id", nullable = false, updatable = false)
    private BookCopy copy;

    @Column(name = "borrower_user_id", nullable = false, updatable = false)
    private UUID borrowerUserId;

    @Column(name = "borrower_name", nullable = false, updatable = false, length = 200)
    private String borrowerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "borrower_type", nullable = false, updatable = false, length = 20)
    private BorrowerType borrowerType;

    @Column(name = "issued_by_user_id", nullable = false, updatable = false)
    private UUID issuedByUserId;

    @Column(name = "returned_by_user_id")
    private UUID returnedByUserId;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Column(name = "returned_at")
    private Instant returnedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_on_issue", updatable = false, length = 20)
    private CopyCondition conditionOnIssue;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_on_return", length = 20)
    private CopyCondition conditionOnReturn;

    @Column(name = "fine_amount", precision = 10, scale = 2)
    private BigDecimal fineAmount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public boolean isReturned() {
        return returnedAt != null;
    }

    public boolean isOverdue(Instant asOf) {
        return !isReturned() && dueAt.isBefore(asOf);
    }

    /** The one allowed mutation — everything else on this row is fixed at insert time. */
    public void recordReturn(UUID returnedByUserId, Instant returnedAt, CopyCondition conditionOnReturn,
            BigDecimal fineAmount) {
        this.returnedByUserId = returnedByUserId;
        this.returnedAt = returnedAt;
        this.conditionOnReturn = conditionOnReturn;
        this.fineAmount = fineAmount;
    }

    /** Renewal only touches due_at — allowed by the DB trigger (not one of the four locked fields). */
    public void extendDueDate(Instant newDueAt) {
        this.dueAt = newDueAt;
    }
}
