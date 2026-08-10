package com.college.erp.module.hr.domain;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 04-database-schema.md §13.3, 03-srs.md [HR-002]. Does NOT extend BaseEntity — unlike every
 * other HR table, `leave_balances` has no `created_by`/`updated_by` columns (see the DDL comment
 * in V011), so this entity hand-rolls the id/deleted/createdAt/updatedAt fields it actually has,
 * the same reasoning as AssetCustodyLog/LibraryCustodyLog not extending BaseEntity.
 */
@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "leave_balances")
public class LeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_type_id", nullable = false, updatable = false)
    private LeaveType leaveType;

    @Column(name = "year", nullable = false, updatable = false)
    private int year;

    @Builder.Default
    @Column(name = "total_days", nullable = false)
    private int totalDays = 0;

    @Builder.Default
    @Column(name = "used_days", nullable = false)
    private int usedDays = 0;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public int remainingDays() {
        return totalDays - usedDays;
    }

    /** [HR-004]: "On final approval, the used_days count is incremented on the leave balance." */
    public void consume(int days) {
        this.usedDays += days;
    }

    /** LeaveRequestService.cancel()/reject() after an already-approved request — gives the days back. */
    public void release(int days) {
        this.usedDays = Math.max(0, this.usedDays - days);
    }
}
