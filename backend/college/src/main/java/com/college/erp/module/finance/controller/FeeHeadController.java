package com.college.erp.module.finance.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.college.erp.common.dto.ApiResponse;
import com.college.erp.module.finance.dto.CreateFeeHeadRequest;
import com.college.erp.module.finance.dto.FeeHeadDto;
import com.college.erp.module.finance.service.FeeHeadService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Not in 05-api-documentation.md's TOC — see FeeHeadService javadoc for why this exists. */
@RestController
@RequestMapping("/api/v1/finance/fee-heads")
@RequiredArgsConstructor
public class FeeHeadController {

    private final FeeHeadService feeHeadService;

    @GetMapping
    @PreAuthorize("hasAnyRole('HEAD_ACCOUNTANT','ACCOUNTANT','ADMIN')")
    public ResponseEntity<ApiResponse<List<FeeHeadDto>>> list() {
        return ResponseEntity.ok(ApiResponse.success(feeHeadService.list()));
    }

    @PostMapping
    @PreAuthorize("hasRole('HEAD_ACCOUNTANT')")
    public ResponseEntity<ApiResponse<FeeHeadDto>> create(@Valid @RequestBody CreateFeeHeadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Fee head created", feeHeadService.create(request)));
    }
}
