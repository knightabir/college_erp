package com.college.erp.module.finance.dto;

import java.math.BigDecimal;

import com.college.erp.module.finance.domain.PaymentMode;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** 05-api-documentation.md §7 POST /finance/fees/invoices/{invoiceId}/collect. */
@Getter
@Setter
public class CollectPaymentRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    private BigDecimal amount;

    @NotNull(message = "Payment mode is required")
    private PaymentMode paymentMode;

    private String transactionReference;
}
