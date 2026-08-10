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
import com.college.erp.module.finance.dto.CreatePurchaseOrderRequest;
import com.college.erp.module.finance.dto.CreateVendorRequest;
import com.college.erp.module.finance.dto.PurchaseOrderDto;
import com.college.erp.module.finance.dto.VendorDto;
import com.college.erp.module.finance.service.ProcurementService;
import com.college.erp.security.jwt.UserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Not in 05-api-documentation.md's TOC — see ProcurementService javadoc for scope (no GRN/three-way-match). */
@RestController
@RequestMapping("/api/v1/finance/procurement")
@RequiredArgsConstructor
public class ProcurementController {

    private final ProcurementService procurementService;

    @PostMapping("/vendors")
    @PreAuthorize("hasAnyRole('HEAD_ACCOUNTANT','ADMIN')")
    public ResponseEntity<ApiResponse<VendorDto>> createVendor(@Valid @RequestBody CreateVendorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Vendor created", procurementService.createVendor(request)));
    }

    @GetMapping("/vendors")
    @PreAuthorize("hasAnyRole('HEAD_ACCOUNTANT','ACCOUNTANT','ADMIN')")
    public ResponseEntity<ApiResponse<List<VendorDto>>> listVendors() {
        return ResponseEntity.ok(ApiResponse.success(procurementService.listActiveVendors()));
    }

    @PostMapping("/purchase-orders")
    @PreAuthorize("hasAnyRole('HEAD_ACCOUNTANT','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<PurchaseOrderDto>> createPurchaseOrder(
            @Valid @RequestBody CreatePurchaseOrderRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                procurementService.createPurchaseOrder(request, principal.getId())));
    }

    @PostMapping("/purchase-orders/{id}/approve")
    @PreAuthorize("hasAnyRole('HEAD_ACCOUNTANT','ADMIN')")
    public ResponseEntity<ApiResponse<PurchaseOrderDto>> approvePurchaseOrder(
            @PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(procurementService.approve(id, principal.getId())));
    }

    @GetMapping("/vendors/{vendorId}/purchase-orders")
    @PreAuthorize("hasAnyRole('HEAD_ACCOUNTANT','ACCOUNTANT','ADMIN')")
    public ResponseEntity<ApiResponse<List<PurchaseOrderDto>>> listByVendor(@PathVariable UUID vendorId) {
        return ResponseEntity.ok(ApiResponse.success(procurementService.listByVendor(vendorId)));
    }
}
