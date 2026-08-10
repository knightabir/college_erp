package com.college.erp.module.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.college.erp.module.finance.domain.FeeInvoice;
import com.college.erp.module.finance.domain.InvoiceStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FeeInvoiceDto {

    private UUID id;
    private String invoiceNumber;
    private UUID studentId;
    private UUID courseId;
    private UUID batchId;
    private int termNumber;
    private UUID academicYearId;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal lateFee;
    private BigDecimal netAmount;
    private BigDecimal paidAmount;
    private BigDecimal balanceDue;
    private LocalDate dueDate;
    private InvoiceStatus status;

    public static FeeInvoiceDto from(FeeInvoice invoice) {
        return FeeInvoiceDto.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .studentId(invoice.getStudent().getId())
                .courseId(invoice.getCourse().getId())
                .batchId(invoice.getBatch().getId())
                .termNumber(invoice.getTermNumber())
                .academicYearId(invoice.getAcademicYear().getId())
                .totalAmount(invoice.getTotalAmount())
                .discountAmount(invoice.getDiscountAmount())
                .lateFee(invoice.getLateFee())
                .netAmount(invoice.getNetAmount())
                .paidAmount(invoice.getPaidAmount())
                .balanceDue(invoice.balanceDue())
                .dueDate(invoice.getDueDate())
                .status(invoice.getStatus())
                .build();
    }
}
