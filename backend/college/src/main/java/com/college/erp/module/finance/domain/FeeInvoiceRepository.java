package com.college.erp.module.finance.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FeeInvoiceRepository extends JpaRepository<FeeInvoice, UUID> {

    Optional<FeeInvoice> findByIdAndDeletedFalse(UUID id);

    List<FeeInvoice> findByStudentIdAndDeletedFalse(UUID studentId);

    List<FeeInvoice> findByStudentIdAndStatusInAndDeletedFalse(UUID studentId, List<InvoiceStatus> statuses);

    boolean existsByStudentIdAndBatchIdAndTermNumberAndDeletedFalse(UUID studentId, UUID batchId, int termNumber);

    boolean existsByInvoiceNumber(String invoiceNumber);

    /** FeeDueDateReminderJob: unpaid/partially-paid invoices due within the reminder window. */
    List<FeeInvoice> findByStatusInAndDueDateBetweenAndDeletedFalse(
            List<InvoiceStatus> statuses, LocalDate from, LocalDate to);
}
