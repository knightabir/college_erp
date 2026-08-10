package com.college.erp.module.library.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.AccessDeniedException;
import com.college.erp.module.library.domain.BookTitle;
import com.college.erp.module.library.domain.Reservation;
import com.college.erp.module.library.domain.ReservationRepository;
import com.college.erp.module.library.domain.ReservationStatus;
import com.college.erp.notification.NotificationRequest;
import com.college.erp.notification.NotificationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 03-srs.md [LIB-007]. Named to match the module's other *ServiceTest files. */
@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private CatalogService catalogService;
    @Mock
    private AuditLogWriter auditLogWriter;
    @Mock
    private NotificationService notificationService;

    private ReservationService reservationService;

    private BookTitle buildTitle() {
        BookTitle title = BookTitle.builder().title("Clean Code").author("Robert C. Martin").build();
        title.setId(UUID.randomUUID());
        return title;
    }

    @Test
    @DisplayName("notifyNextInQueue() — no active reservations: returns false, nothing notified")
    void notifyNextInQueue_noReservations_returnsFalse() {
        // ARRANGE
        reservationService = new ReservationService(reservationRepository, catalogService, auditLogWriter,
                notificationService);
        UUID titleId = UUID.randomUUID();
        when(reservationRepository.findActiveByTitleOrderByQueuePosition(titleId)).thenReturn(List.of());

        // ACT
        boolean result = reservationService.notifyNextInQueue(titleId);

        // ASSERT
        assertThat(result).isFalse();
        verify(notificationService, never()).send(any());
    }

    @Test
    @DisplayName("notifyNextInQueue() — activates the head of the queue, sets a 48h pickup window, and notifies them")
    void notifyNextInQueue_activatesHeadAndNotifies() {
        // ARRANGE
        reservationService = new ReservationService(reservationRepository, catalogService, auditLogWriter,
                notificationService);
        BookTitle title = buildTitle();
        UUID userId = UUID.randomUUID();
        Reservation reservation = Reservation.builder().title(title).userId(userId).queuePosition(1)
                .status(ReservationStatus.WAITING).build();
        reservation.setId(UUID.randomUUID());
        when(reservationRepository.findActiveByTitleOrderByQueuePosition(title.getId()))
                .thenReturn(List.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));
        ArgumentCaptor<NotificationRequest> requestCaptor = ArgumentCaptor.forClass(NotificationRequest.class);

        // ACT
        boolean result = reservationService.notifyNextInQueue(title.getId());

        // ASSERT
        assertThat(result).isTrue();
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.READY);
        assertThat(reservation.getNotifiedAt()).isNotNull();
        assertThat(reservation.getExpiresAt()).isNotNull();
        verify(notificationService).send(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getUserId()).isEqualTo(userId);
        assertThat(requestCaptor.getValue().getBody()).contains("Clean Code");
    }

    @Test
    @DisplayName("cancel() — a reservation belonging to someone else is rejected")
    void cancel_notOwner_throws() {
        // ARRANGE
        reservationService = new ReservationService(reservationRepository, catalogService, auditLogWriter,
                notificationService);
        Reservation reservation = Reservation.builder().title(buildTitle()).userId(UUID.randomUUID())
                .queuePosition(1).build();
        UUID reservationId = UUID.randomUUID();
        reservation.setId(reservationId);
        when(reservationRepository.findByIdAndDeletedFalse(reservationId)).thenReturn(Optional.of(reservation));

        // ACT / ASSERT
        assertThatThrownBy(() -> reservationService.cancel(reservationId, UUID.randomUUID()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("cancel() — the owner may cancel their own reservation")
    void cancel_owner_succeeds() {
        // ARRANGE
        reservationService = new ReservationService(reservationRepository, catalogService, auditLogWriter,
                notificationService);
        UUID userId = UUID.randomUUID();
        Reservation reservation = Reservation.builder().title(buildTitle()).userId(userId).queuePosition(1).build();
        UUID reservationId = UUID.randomUUID();
        reservation.setId(reservationId);
        when(reservationRepository.findByIdAndDeletedFalse(reservationId)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        // ACT
        reservationService.cancel(reservationId, userId);

        // ASSERT
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }
}
