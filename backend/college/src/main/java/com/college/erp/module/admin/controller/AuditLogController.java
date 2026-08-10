package com.college.erp.module.admin.controller;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.college.erp.audit.AuditAction;
import com.college.erp.common.dto.ApiResponse;
import com.college.erp.common.dto.PageResponse;
import com.college.erp.module.admin.dto.AuditLogDto;
import com.college.erp.module.admin.service.AuditLogService;

import lombok.RequiredArgsConstructor;

/** 05-api-documentation.md §13 "Audit Log", 03-srs.md [AUDIT-004]. */
@RestController
@RequestMapping("/api/v1/admin/audit-log")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','AUDITOR')")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogDto>>> search(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            Pageable pageable) {
        Page<AuditLogDto> page = auditLogService.search(userId, entityType, action, from, to, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(page)));
    }
}
