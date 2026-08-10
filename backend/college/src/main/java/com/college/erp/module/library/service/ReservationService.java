package com.college.erp.module.library.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.AccessDeniedException;
import com.college.erp.common.exception.BusinessRuleViolationException;
import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.module.library.domain.BookTitle;
import com.college.erp.module.library.domain.Reservation;
import com.college.erp.module.library.domain.ReservationRepository;
import com.college.erp.module.library.domain.ReservationStatus;
import com.college.erp.module.library.dto.ReservationDto;
import com.college.erp.notification.NotificationRequest;
import com.college.erp.notification.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 03-srs.md [LIB-007]. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private static final String MODULE = "LIBRARY";
    private static final int PICKUP_WINDOW_HOURS = 48; // "default: 48 hours" per LIB-007

    private final ReservationRepository reservationRepository;
    private final CatalogService catalogService;
    private final AuditLogWriter auditLogWriter;
    private final NotificationService notificationService;

    @Transactional
    public ReservationDto join(UUID titleId, UUID userId) {
        BookTitle title = catalogService.getTitleEntity(titleId);
        if (title.getAvailableCopies() > 0) {
            throw new BusinessRuleViolationException("Copies are currently available — no need to reserve");
        }

        long queuePosition = reservationRepository.countByTitleIdAndStatusInAndDeletedFalse(
                titleId, List.of(ReservationStatus.WAITING, ReservationStatus.READY)) + 1;

        Reservation reservation = Reservation.builder()
                .title(title)
                .userId(userId)
                .queuePosition((int) queuePosition)
                .build();

        Reservation saved = reservationRepository.save(reservation);
        auditLogWriter.write(AuditAction.CREATE, MODULE, "Reservation", saved.getId().toString(),
                "Reservation queued at position " + queuePosition + " for " + title.getTitle());

        return ReservationDto.from(saved);
    }

    @Transactional
    public void cancel(UUID id, UUID requestingUserId) {
        Reservation reservation = getEntity(id);
        if (!reservation.getUserId().equals(requestingUserId)) {
            throw new AccessDeniedException("This reservation does not belong to you");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);
        auditLogWriter.write(AuditAction.UPDATE, MODULE, "Reservation", id.toString(), "Cancelled");
    }

    @Transactional(readOnly = true)
    public boolean hasActiveReservation(UUID titleId) {
        return reservationRepository.existsActiveByTitle(titleId);
    }

    /** Called from CirculationService.returnBook() when a copy becomes free. Returns true if a
     *  reservation was activated (and the copy should be held as RESERVED rather than AVAILABLE). */
    @Transactional
    public boolean notifyNextInQueue(UUID titleId) {
        List<Reservation> active = reservationRepository.findActiveByTitleOrderByQueuePosition(titleId);
        if (active.isEmpty()) {
            return false;
        }

        Reservation next = active.get(0);
        Instant now = Instant.now();
        next.setStatus(ReservationStatus.READY);
        next.setNotifiedAt(now);
        next.setExpiresAt(now.plus(PICKUP_WINDOW_HOURS, ChronoUnit.HOURS));
        reservationRepository.save(next);

        notificationService.send(NotificationRequest.builder()
                .userId(next.getUserId())
                .title("Reserved Book Ready for Pickup")
                .body(next.getTitle().getTitle() + " is ready for pickup — reserved until "
                        + next.getExpiresAt() + ".")
                .referenceType("Reservation").referenceId(next.getId().toString())
                .build());
        log.info("Reservation [{}] for user [{}] is ready for pickup (expires {})",
                next.getId(), next.getUserId(), next.getExpiresAt());

        return true;
    }

    private Reservation getEntity(UUID id) {
        return reservationRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", id));
    }
}
