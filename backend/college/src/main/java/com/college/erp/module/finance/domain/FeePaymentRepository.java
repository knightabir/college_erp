package com.college.erp.module.finance.domain;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FeePaymentRepository extends JpaRepository<FeePayment, UUID> {

    List<FeePayment> findByInvoiceIdAndDeletedFalse(UUID invoiceId);

    boolean existsByReceiptNumber(String receiptNumber);
}
