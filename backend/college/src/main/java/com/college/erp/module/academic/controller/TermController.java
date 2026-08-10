package com.college.erp.module.academic.controller;

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
import com.college.erp.module.academic.dto.CreateTermRequest;
import com.college.erp.module.academic.dto.TermDto;
import com.college.erp.module.academic.service.TermService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Not in 05-api-documentation.md's TOC — see AcademicYearService javadoc for why this exists. */
@RestController
@RequestMapping("/api/v1/academic/terms")
@RequiredArgsConstructor
public class TermController {

    private final TermService termService;

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TermDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(termService.getById(id)));
    }

    @GetMapping("/by-academic-year/{academicYearId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<TermDto>>> listByAcademicYear(@PathVariable UUID academicYearId) {
        return ResponseEntity.ok(ApiResponse.success(termService.listByAcademicYear(academicYearId)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ApiResponse<TermDto>> create(@Valid @RequestBody CreateTermRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Term created", termService.create(request)));
    }
}
