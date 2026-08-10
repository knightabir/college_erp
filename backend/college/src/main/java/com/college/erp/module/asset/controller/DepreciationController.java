package com.college.erp.module.asset.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.college.erp.common.dto.ApiResponse;
import com.college.erp.module.asset.dto.DepreciationDto;
import com.college.erp.module.asset.service.DepreciationService;

import lombok.RequiredArgsConstructor;

/**
 * 03-srs.md [AST-005]. Not in 05-api-documentation.md's TOC — see DepreciationService javadoc for
 * why this is an on-demand calculation rather than a persisted schedule.
 */
@RestController
@RequestMapping("/api/v1/assets")
@RequiredArgsConstructor
public class DepreciationController {

    private final DepreciationService depreciationService;

    @GetMapping("/{id}/depreciation")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','ASSET_MANAGER')")
    public ResponseEntity<ApiResponse<DepreciationDto>> calculate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(depreciationService.calculateCurrentValue(id)));
    }
}
