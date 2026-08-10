package com.college.erp.module.communication.domain;

import java.time.Instant;
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

/** 04-database-schema.md §15.1, 03-srs.md [COMM-001]. */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "notices")
public class Notice extends BaseEntity {

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private NoticeTargetType targetType = NoticeTargetType.ALL;

    /** department_id or batch_id when targeted — see class-level note on ROLE targeting not
     *  being filterable through this column (module's CLAUDE.md deviations). */
    @Column(name = "target_id")
    private UUID targetId;

    @Builder.Default
    @Column(name = "is_pinned", nullable = false)
    private boolean pinned = false;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    public boolean isCurrentlyVisible(Instant asOf) {
        boolean published = publishedAt != null && !publishedAt.isAfter(asOf);
        boolean notExpired = expiresAt == null || expiresAt.isAfter(asOf);
        return published && notExpired;
    }
}
