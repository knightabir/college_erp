package com.college.erp.module.library.dto;

import java.time.Instant;
import java.util.UUID;

import com.college.erp.module.library.domain.Reservation;
import com.college.erp.module.library.domain.ReservationStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReservationDto {

    private UUID id;
    private UUID titleId;
    private UUID userId;
    private int queuePosition;
    private ReservationStatus status;
    private Instant notifiedAt;
    private Instant expiresAt;

    public static ReservationDto from(Reservation reservation) {
        return ReservationDto.builder()
                .id(reservation.getId())
                .titleId(reservation.getTitle().getId())
                .userId(reservation.getUserId())
                .queuePosition(reservation.getQueuePosition())
                .status(reservation.getStatus())
                .notifiedAt(reservation.getNotifiedAt())
                .expiresAt(reservation.getExpiresAt())
                .build();
    }
}
