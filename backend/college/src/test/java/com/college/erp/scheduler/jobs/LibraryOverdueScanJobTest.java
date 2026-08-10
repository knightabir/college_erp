package com.college.erp.scheduler.jobs;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.college.erp.module.library.domain.BookCopy;
import com.college.erp.module.library.domain.BookTitle;
import com.college.erp.module.library.domain.BorrowerType;
import com.college.erp.module.library.domain.LibraryCustodyLog;
import com.college.erp.module.library.service.CirculationService;
import com.college.erp.notification.NotificationRequest;
import com.college.erp.notification.NotificationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LibraryOverdueScanJobTest {

    @Mock
    private CirculationService circulationService;
    @Mock
    private NotificationService notificationService;

    private LibraryOverdueScanJob job;

    @Test
    @DisplayName("jobType() matches the JobTypes constant")
    void jobType_matchesConstant() {
        job = new LibraryOverdueScanJob(circulationService, notificationService);
        assertThat(job.jobType()).isEqualTo(JobTypes.LIBRARY_OVERDUE_SCAN);
    }

    @Test
    @DisplayName("handle() — notifies every overdue borrower directly (borrowerUserId is already a users(id))")
    void handle_notifiesEveryOverdueBorrower() {
        // ARRANGE
        job = new LibraryOverdueScanJob(circulationService, notificationService);
        BookTitle title = BookTitle.builder().title("Clean Code").author("Robert C. Martin").build();
        BookCopy copy = BookCopy.builder().title(title).barcode("LIB-2026-000001")
                .accessionNumber("2026-000001").build();
        UUID borrowerUserId = UUID.randomUUID();
        LibraryCustodyLog overdueLog = LibraryCustodyLog.builder()
                .id(UUID.randomUUID())
                .copy(copy).borrowerUserId(borrowerUserId).borrowerName("Arjun Kumar")
                .borrowerType(BorrowerType.STUDENT).issuedByUserId(UUID.randomUUID())
                .issuedAt(Instant.now().minus(20, ChronoUnit.DAYS))
                .dueAt(Instant.now().minus(6, ChronoUnit.DAYS))
                .build();
        when(circulationService.listAllOverdueEntities()).thenReturn(List.of(overdueLog));

        // ACT
        job.handle("{}");

        // ASSERT
        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService).send(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(borrowerUserId);
        assertThat(captor.getValue().getBody()).contains("Clean Code").contains("LIB-2026-000001");
    }
}
