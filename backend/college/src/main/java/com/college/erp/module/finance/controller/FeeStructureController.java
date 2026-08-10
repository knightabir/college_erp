package com.college.erp.module.finance.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.college.erp.common.dto.ApiResponse;
import com.college.erp.module.finance.dto.CreateFeeStructureRequest;
import com.college.erp.module.finance.dto.FeeStructureDto;
import com.college.erp.module.finance.service.FeeStructureService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** 05-api-documentation.md §7 "Fee Structure". */
@RestController
@RequestMapping("/api/v1/finance/fee-structures")
@RequiredArgsConstructor
public class FeeStructureController {

    private final FeeStructureService feeStructureService;

    @GetMapping("/course/{courseId}")
    @PreAuthorize("hasAnyRole('HEAD_ACCOUNTANT','ACCOUNTANT','ADMIN')")
    public ResponseEntity<ApiResponse<List<FeeStructureDto>>> listByCourse(@PathVariable UUID courseId) {
        return ResponseEntity.ok(ApiResponse.success(feeStructureService.listByCourse(courseId)));
    }

    @PostMapping
    @PreAuthorize("hasRole('HEAD_ACCOUNTANT')")
    public ResponseEntity<ApiResponse<FeeStructureDto>> create(
            @Valid @RequestBody CreateFeeStructureRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Fee structure created", feeStructureService.create(request)));
    }
}
