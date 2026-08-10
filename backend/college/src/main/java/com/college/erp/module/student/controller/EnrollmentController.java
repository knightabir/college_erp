package com.college.erp.module.student.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.college.erp.common.dto.ApiResponse;
import com.college.erp.module.student.dto.EnrollmentDto;
import com.college.erp.module.student.dto.TransferSectionRequest;
import com.college.erp.module.student.service.EnrollmentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** 05-api-documentation.md §4 — GET .../enrollment, GET .../enrollment/history, POST .../transfer-section. */
@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @GetMapping("/{id}/enrollment")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HOD','FACULTY') "
            + "or @studentSecurityService.isOwnProfile(#id, principal)")
    public ResponseEntity<ApiResponse<EnrollmentDto>> getCurrent(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(enrollmentService.getCurrent(id)));
    }

    @GetMapping("/{id}/enrollment/history")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HOD')")
    public ResponseEntity<ApiResponse<List<EnrollmentDto>>> getHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(enrollmentService.getHistory(id)));
    }

    @PostMapping("/{id}/transfer-section")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HOD')")
    public ResponseEntity<ApiResponse<EnrollmentDto>> transferSection(
            @PathVariable UUID id, @Valid @RequestBody TransferSectionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(enrollmentService.transferSection(id, request)));
    }
}
