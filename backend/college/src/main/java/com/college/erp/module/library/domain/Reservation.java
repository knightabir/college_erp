package com.college.erp.module.library.domain;

import java.time.Instant;
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

/** 04-database-schema.md §11.5, 03-srs.md [LIB-007]. */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "reservations", indexes = @Index(name = "idx_reservation_title", columnList = "title_id"))
public class Reservation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "title_id", nullable = false)
    private BookTitle title;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "queue_position", nullable = false)
    private int queuePosition;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReservationStatus status = ReservationStatus.WAITING;

    @Column(name = "notified_at")
    private Instant notifiedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;
}
