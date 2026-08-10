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
import com.college.erp.module.finance.dto.AccountHeadDto;
import com.college.erp.module.finance.dto.CreateAccountHeadRequest;
import com.college.erp.module.finance.service.AccountHeadService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Not in 05-api-documentation.md's TOC — see AccountHeadService javadoc for why this exists. */
@RestController
@RequestMapping("/api/v1/finance/account-heads")
@RequiredArgsConstructor
public class AccountHeadController {

    private final AccountHeadService accountHeadService;

    @GetMapping
    @PreAuthorize("hasAnyRole('HEAD_ACCOUNTANT','ACCOUNTANT','AUDITOR')")
    public ResponseEntity<ApiResponse<List<AccountHeadDto>>> list() {
        return ResponseEntity.ok(ApiResponse.success(accountHeadService.list()));
    }

    @PostMapping
    @PreAuthorize("hasRole('HEAD_ACCOUNTANT')")
    public ResponseEntity<ApiResponse<AccountHeadDto>> create(@Valid @RequestBody CreateAccountHeadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account head created", accountHeadService.create(request)));
    }
}
