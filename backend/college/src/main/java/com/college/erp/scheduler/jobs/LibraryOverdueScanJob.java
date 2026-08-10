package com.college.erp.scheduler.jobs;

import java.util.List;

import org.springframework.stereotype.Component;

import com.college.erp.module.library.domain.LibraryCustodyLog;
import com.college.erp.module.library.service.CirculationService;
import com.college.erp.notification.NotificationRequest;
import com.college.erp.notification.NotificationService;
import com.college.erp.scheduler.JobHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 03-srs.md [SCHED-004]: "Library overdue scan: nightly at 02:00." Notifies every borrower with
 * at least one overdue, unreturned loan — LibraryCustodyLog.borrowerUserId is already a users(id)
 * reference (see CirculationService.listAllOverdueEntities()), no extra lookup needed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LibraryOverdueScanJob implements JobHandler {

    private final CirculationService circulationService;
    private final NotificationService notificationService;

    @Override
    public String jobType() {
        return JobTypes.LIBRARY_OVERDUE_SCAN;
    }

    @Override
    public void handle(String payloadJson) {
        List<LibraryCustodyLog> overdue = circulationService.listAllOverdueEntities();
        for (LibraryCustodyLog custodyLog : overdue) {
            notificationService.send(NotificationRequest.builder()
                    .userId(custodyLog.getBorrowerUserId())
                    .title("Overdue Library Book")
                    .body(custodyLog.getCopy().getTitle().getTitle() + " (barcode " + custodyLog.getCopy().getBarcode()
                            + ") was due " + custodyLog.getDueAt() + " and hasn't been returned yet.")
                    .referenceType("LibraryCustodyLog").referenceId(custodyLog.getId().toString())
                    .build());
        }
        log.info("LibraryOverdueScanJob: notified {} overdue borrower(s)", overdue.size());
    }
}
