package com.college.erp.module.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePurchaseOrderRequest {

    @NotNull(message = "Vendor is required")
    private UUID vendorId;

    private UUID departmentId;

    @NotNull(message = "Total amount is required")
    private BigDecimal totalAmount;

    private String description;

    @NotNull(message = "PO date is required")
    private LocalDate poDate;

    private LocalDate expectedDeliveryDate;
}
