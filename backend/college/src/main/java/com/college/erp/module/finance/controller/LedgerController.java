package com.college.erp.module.finance.controller;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.college.erp.common.dto.ApiResponse;
import com.college.erp.common.dto.PageResponse;
import com.college.erp.module.finance.dto.LedgerEntryDto;
import com.college.erp.module.finance.dto.ManualLedgerEntryRequest;
import com.college.erp.module.finance.service.FinancialYearService;
import com.college.erp.module.finance.service.LedgerService;
import com.college.erp.security.jwt.UserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** 05-api-documentation.md §7 "Ledger". */
@RestController
@RequestMapping("/api/v1/finance/ledger")
@RequiredArgsConstructor
public class LedgerController {

    private final LedgerService ledgerService;
    private final FinancialYearService financialYearService;

    @GetMapping
    @PreAuthorize("hasAnyRole('HEAD_ACCOUNTANT','AUDITOR')")
    public ResponseEntity<ApiResponse<PageResponse<LedgerEntryDto>>> browse(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Pageable pageable) {
        Page<LedgerEntryDto> page = ledgerService.browse(from, to, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(page)));
    }

    @PostMapping("/manual-entry")
    @PreAuthorize("hasRole('HEAD_ACCOUNTANT')")
    public ResponseEntity<ApiResponse<LedgerEntryDto>> postManualEntry(
            @Valid @RequestBody ManualLedgerEntryRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        var financialYear = financialYearService.getCurrentEntity();
        LedgerEntryDto entry = ledgerService.postManualEntry(request, financialYear, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(entry));
    }
}
