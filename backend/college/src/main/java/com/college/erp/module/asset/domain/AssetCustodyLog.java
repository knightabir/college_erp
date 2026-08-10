package com.college.erp.module.asset.domain;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * 04-database-schema.md §12.3, 03-srs.md [AST-002]. Fully immutable (unlike library's
 * LibraryCustodyLog, which allows a later recordReturn()) — every field is fixed at construction
 * and there is no mutator anywhere on this class, mirroring the DB-level
 * trg_acl_no_update/trg_acl_no_delete triggers that reuse fn_reject_modification().
 */
@Getter
@Entity
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "asset_custody_log",
    indexes = {
        @Index(name = "idx_acl_asset", columnList = "asset_id"),
        @Index(name = "idx_acl_assignee", columnList = "assigned_to_user_id")
    }
)
public class AssetCustodyLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false, updatable = false)
    private Asset asset;

    @Column(name = "from_department_id", updatable = false)
    private UUID fromDepartmentId;

    @Column(name = "to_department_id", nullable = false, updatable = false)
    private UUID toDepartmentId;

    @Column(name = "from_user_id", updatable = false)
    private UUID fromUserId;

    @Column(name = "assigned_to_user_id", updatable = false)
    private UUID assignedToUserId;

    @Column(name = "transferred_by_user_id", nullable = false, updatable = false)
    private UUID transferredByUserId;

    @Column(name = "transferred_at", nullable = false, updatable = false)
    private Instant transferredAt;

    @Column(name = "reason", columnDefinition = "TEXT", updatable = false)
    private String reason;

    @Column(name = "from_location", length = 100, updatable = false)
    private String fromLocation;

    @Column(name = "to_location", length = 100, updatable = false)
    private String toLocation;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
