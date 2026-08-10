package com.college.erp.module.communication.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.college.erp.common.dto.ApiResponse;
import com.college.erp.module.communication.dto.CreateNoticeRequest;
import com.college.erp.module.communication.dto.NoticeDto;
import com.college.erp.module.communication.service.NoticeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** 05-api-documentation.md §11 "Communication" (notices half). */
@RestController
@RequestMapping("/api/v1/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HOD')")
    public ResponseEntity<ApiResponse<NoticeDto>> create(@Valid @RequestBody CreateNoticeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(noticeService.create(request)));
    }

    /** See NoticeService.getRelevant() javadoc — departmentId/batchId are optional query params,
     *  not resolved automatically from the caller's identity. */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<NoticeDto>>> list(
            @RequestParam(required = false) UUID departmentId, @RequestParam(required = false) UUID batchId) {
        return ResponseEntity.ok(ApiResponse.success(noticeService.getRelevant(departmentId, batchId)));
    }
}
