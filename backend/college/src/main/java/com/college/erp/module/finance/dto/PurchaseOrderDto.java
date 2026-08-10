package com.college.erp.module.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.college.erp.module.finance.domain.PurchaseOrder;
import com.college.erp.module.finance.domain.PurchaseOrderStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PurchaseOrderDto {

    private UUID id;
    private String poNumber;
    private UUID vendorId;
    private String vendorName;
    private UUID departmentId;
    private UUID financialYearId;
    private BigDecimal totalAmount;
    private String description;
    private LocalDate poDate;
    private LocalDate expectedDeliveryDate;
    private PurchaseOrderStatus status;
    private UUID approvedByUserId;

    public static PurchaseOrderDto from(PurchaseOrder po) {
        return PurchaseOrderDto.builder()
                .id(po.getId())
                .poNumber(po.getPoNumber())
                .vendorId(po.getVendor().getId())
                .vendorName(po.getVendor().getName())
                .departmentId(po.getDepartmentId())
                .financialYearId(po.getFinancialYear().getId())
                .totalAmount(po.getTotalAmount())
                .description(po.getDescription())
                .poDate(po.getPoDate())
                .expectedDeliveryDate(po.getExpectedDeliveryDate())
                .status(po.getStatus())
                .approvedByUserId(po.getApprovedByUserId())
                .build();
    }
}
