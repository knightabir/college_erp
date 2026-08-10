package com.college.erp.scheduler.jobs;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.college.erp.module.finance.domain.FeeInvoice;
import com.college.erp.module.finance.service.FeeCollectionService;
import com.college.erp.module.finance.domain.InvoiceStatus;
import com.college.erp.module.student.domain.Student;
import com.college.erp.notification.NotificationRequest;
import com.college.erp.notification.NotificationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeeDueDateReminderJobTest {

    @Mock
    private FeeCollectionService feeCollectionService;
    @Mock
    private NotificationService notificationService;

    private FeeDueDateReminderJob job;

    private Student buildStudent(UUID userId) {
        Student student = Student.builder().enrollmentNumber("2024CSE001").firstName("Arjun")
                .lastName("Kumar").userId(userId).build();
        student.setId(UUID.randomUUID());
        return student;
    }

    @Test
    @DisplayName("jobType() matches the JobTypes constant")
    void jobType_matchesConstant() {
        job = new FeeDueDateReminderJob(feeCollectionService, notificationService);
        assertThat(job.jobType()).isEqualTo(JobTypes.FEE_DUE_DATE_REMINDER);
    }

    @Test
    @DisplayName("handle() — notifies the student for each due invoice with a linked user account")
    void handle_notifiesStudentsWithLinkedAccount() {
        // ARRANGE
        job = new FeeDueDateReminderJob(feeCollectionService, notificationService);
        UUID userId = UUID.randomUUID();
        Student student = buildStudent(userId);
        FeeInvoice invoice = FeeInvoice.builder()
                .invoiceNumber("INV-2026-001").student(student).dueDate(LocalDate.now().plusDays(3))
                .netAmount(BigDecimal.valueOf(25000)).paidAmount(BigDecimal.ZERO)
                .status(InvoiceStatus.UNPAID).build();
        invoice.setId(UUID.randomUUID());
        when(feeCollectionService.listDueSoonEntities(anyList(), any(), any()))
                .thenReturn(List.of(invoice));

        // ACT
        job.handle("{}");

        // ASSERT
        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService).send(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(captor.getValue().getBody()).contains("INV-2026-001").contains("25000");
    }

    @Test
    @DisplayName("handle() — an invoice whose student has no linked user account is skipped, not an error")
    void handle_noLinkedAccount_isSkipped() {
        // ARRANGE
        job = new FeeDueDateReminderJob(feeCollectionService, notificationService);
        Student student = buildStudent(null);
        FeeInvoice invoice = FeeInvoice.builder()
                .invoiceNumber("INV-2026-002").student(student).dueDate(LocalDate.now().plusDays(1))
                .netAmount(BigDecimal.valueOf(10000)).paidAmount(BigDecimal.ZERO)
                .status(InvoiceStatus.UNPAID).build();
        invoice.setId(UUID.randomUUID());
        when(feeCollectionService.listDueSoonEntities(anyList(), any(), any()))
                .thenReturn(List.of(invoice));

        // ACT
        job.handle("{}");

        // ASSERT
        verify(notificationService, never()).send(any());
    }
}
