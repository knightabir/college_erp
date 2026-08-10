package com.college.erp.module.finance.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.college.erp.common.dto.ApiResponse;
import com.college.erp.module.finance.dto.CollectPaymentRequest;
import com.college.erp.module.finance.dto.FeeInvoiceDto;
import com.college.erp.module.finance.dto.GenerateInvoiceRequest;
import com.college.erp.module.finance.dto.ReceiptDto;
import com.college.erp.module.finance.service.FeeCollectionService;
import com.college.erp.security.jwt.UserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** 05-api-documentation.md §7 "Fee Invoices" / "Fee Payments". */
@RestController
@RequestMapping("/api/v1/finance/fees")
@RequiredArgsConstructor
public class FeeCollectionController {

    private final FeeCollectionService feeCollectionService;

    @PostMapping("/invoices/generate")
    @PreAuthorize("hasAnyRole('HEAD_ACCOUNTANT','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<FeeInvoiceDto>> generateInvoice(
            @Valid @RequestBody GenerateInvoiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(feeCollectionService.generateInvoice(request)));
    }

    @GetMapping("/students/{studentId}/invoices")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HEAD_ACCOUNTANT','ACCOUNTANT') "
            + "or @studentSecurityService.isOwnProfile(#studentId, principal)")
    public ResponseEntity<ApiResponse<List<FeeInvoiceDto>>> getStudentInvoices(@PathVariable UUID studentId) {
        return ResponseEntity.ok(ApiResponse.success(feeCollectionService.getStudentInvoices(studentId)));
    }

    @GetMapping("/students/{studentId}/outstanding")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HEAD_ACCOUNTANT','ACCOUNTANT') "
            + "or @studentSecurityService.isOwnProfile(#studentId, principal)")
    public ResponseEntity<ApiResponse<List<FeeInvoiceDto>>> getOutstanding(@PathVariable UUID studentId) {
        return ResponseEntity.ok(ApiResponse.success(feeCollectionService.getOutstanding(studentId)));
    }

    @PostMapping("/invoices/{invoiceId}/collect")
    @PreAuthorize("hasAnyRole('HEAD_ACCOUNTANT','ACCOUNTANT','FEE_COLLECTION_CLERK')")
    public ResponseEntity<ApiResponse<ReceiptDto>> collectPayment(
            @PathVariable UUID invoiceId, @Valid @RequestBody CollectPaymentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ReceiptDto receipt = feeCollectionService.collectPayment(invoiceId, request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment recorded — receipt: " + receipt.getReceiptNumber(), receipt));
    }

    @GetMapping("/invoices/{invoiceId}/payments")
    @PreAuthorize("hasAnyRole('HEAD_ACCOUNTANT','ACCOUNTANT','AUDITOR')")
    public ResponseEntity<ApiResponse<List<ReceiptDto>>> getPayments(@PathVariable UUID invoiceId) {
        return ResponseEntity.ok(ApiResponse.success(feeCollectionService.getPayments(invoiceId)));
    }
}
