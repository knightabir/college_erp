package com.college.erp.module.finance.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.college.erp.common.dto.ApiResponse;
import com.college.erp.module.finance.dto.AccountantGrantDto;
import com.college.erp.module.finance.dto.CreateAccountantGrantRequest;
import com.college.erp.module.finance.service.AccountantGrantService;
import com.college.erp.security.jwt.UserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** 05-api-documentation.md §7 "Accountant Permission Grants" — all HEAD_ACCOUNTANT-only. */
@RestController
@RequestMapping("/api/v1/finance/accountant-grants")
@RequiredArgsConstructor
public class AccountantGrantController {

    private final AccountantGrantService accountantGrantService;

    @PostMapping
    @PreAuthorize("hasRole('HEAD_ACCOUNTANT')")
    public ResponseEntity<ApiResponse<AccountantGrantDto>> grant(
            @Valid @RequestBody CreateAccountantGrantRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(accountantGrantService.grant(request, principal.getId())));
    }

    @GetMapping("/my-grants")
    @PreAuthorize("hasRole('HEAD_ACCOUNTANT')")
    public ResponseEntity<ApiResponse<List<AccountantGrantDto>>> myGrants(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(accountantGrantService.listGrantedBy(principal.getId())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('HEAD_ACCOUNTANT')")
    public ResponseEntity<ApiResponse<Void>> revoke(
            @PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        accountantGrantService.revoke(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Grant revoked", null));
    }
}
