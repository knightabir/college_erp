package com.college.erp.scheduler.jobs;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;

import com.college.erp.module.finance.domain.FeeInvoice;
import com.college.erp.module.finance.domain.InvoiceStatus;
import com.college.erp.module.finance.service.FeeCollectionService;
import com.college.erp.notification.NotificationRequest;
import com.college.erp.notification.NotificationService;
import com.college.erp.scheduler.JobHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 03-srs.md [SCHED-004]: "Fee due-date reminder sweep: daily at 08:00." Notifies the student
 * (Student.userId) for every UNPAID/PARTIAL invoice due within the next REMINDER_WINDOW_DAYS.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeeDueDateReminderJob implements JobHandler {

    private static final int REMINDER_WINDOW_DAYS = 7;
    private static final List<InvoiceStatus> REMINDABLE_STATUSES = List.of(
            InvoiceStatus.UNPAID, InvoiceStatus.PARTIAL);

    private final FeeCollectionService feeCollectionService;
    private final NotificationService notificationService;

    @Override
    public String jobType() {
        return JobTypes.FEE_DUE_DATE_REMINDER;
    }

    @Override
    public void handle(String payloadJson) {
        LocalDate today = LocalDate.now();
        List<FeeInvoice> due = feeCollectionService.listDueSoonEntities(
                REMINDABLE_STATUSES, today, today.plusDays(REMINDER_WINDOW_DAYS));

        int notified = 0;
        for (FeeInvoice invoice : due) {
            var userId = invoice.getStudent().getUserId();
            if (userId == null) {
                continue;
            }
            notificationService.send(NotificationRequest.builder()
                    .userId(userId)
                    .title("Fee Payment Reminder")
                    .body("Your invoice " + invoice.getInvoiceNumber() + " has an outstanding balance of "
                            + invoice.balanceDue() + ", due " + invoice.getDueDate() + ".")
                    .referenceType("FeeInvoice").referenceId(invoice.getId().toString())
                    .build());
            notified++;
        }
        log.info("FeeDueDateReminderJob: notified {} of {} due invoice(s)", notified, due.size());
    }
}
