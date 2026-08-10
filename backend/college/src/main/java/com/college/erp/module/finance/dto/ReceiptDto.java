package com.college.erp.module.finance.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.college.erp.module.finance.domain.FeePayment;
import com.college.erp.module.finance.domain.PaymentMode;
import com.college.erp.module.finance.domain.PaymentStatus;

import lombok.Builder;
import lombok.Getter;

/** 05-api-documentation.md §7 POST .../collect response shape. */
@Getter
@Builder
public class ReceiptDto {

    private UUID id;
    private UUID invoiceId;
    private String receiptNumber;
    private BigDecimal amount;
    private PaymentMode paymentMode;
    private String transactionReference;
    private Instant paymentTimestamp;
    private PaymentStatus status;

    public static ReceiptDto from(FeePayment payment) {
        return ReceiptDto.builder()
                .id(payment.getId())
                .invoiceId(payment.getInvoice().getId())
                .receiptNumber(payment.getReceiptNumber())
                .amount(payment.getAmount())
                .paymentMode(payment.getPaymentMode())
                .transactionReference(payment.getTransactionReference())
                .paymentTimestamp(payment.getPaymentTimestamp())
                .status(payment.getStatus())
                .build();
    }
}
