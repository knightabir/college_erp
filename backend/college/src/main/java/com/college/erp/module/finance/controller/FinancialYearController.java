package com.college.erp.module.finance.controller;

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
import com.college.erp.module.finance.dto.CreateFinancialYearRequest;
import com.college.erp.module.finance.dto.FinancialYearDto;
import com.college.erp.module.finance.service.FinancialYearService;
import com.college.erp.security.jwt.UserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** 05-api-documentation.md §7 "Financial Year". */
@RestController
@RequestMapping("/api/v1/finance/financial-years")
@RequiredArgsConstructor
public class FinancialYearController {

    private final FinancialYearService financialYearService;

    @GetMapping("/current")
    @PreAuthorize("hasAnyRole('HEAD_ACCOUNTANT','ACCOUNTANT','AUDITOR','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<FinancialYearDto>> getCurrent() {
        return ResponseEntity.ok(ApiResponse.success(financialYearService.getCurrent()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('HEAD_ACCOUNTANT','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<FinancialYearDto>> create(
            @Valid @RequestBody CreateFinancialYearRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Financial year created", financialYearService.create(request)));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('HEAD_ACCOUNTANT','ADMIN')")
    public ResponseEntity<ApiResponse<FinancialYearDto>> close(
            @PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                financialYearService.initiateOrCompleteClose(id, principal.getId())));
    }
}
