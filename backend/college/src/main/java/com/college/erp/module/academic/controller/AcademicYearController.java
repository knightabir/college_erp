package com.college.erp.module.academic.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.college.erp.common.dto.ApiResponse;
import com.college.erp.common.dto.PageResponse;
import com.college.erp.module.academic.dto.AcademicYearDto;
import com.college.erp.module.academic.dto.CreateAcademicYearRequest;
import com.college.erp.module.academic.service.AcademicYearService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Not in 05-api-documentation.md's TOC — see AcademicYearService javadoc for why this exists. */
@RestController
@RequestMapping("/api/v1/academic/academic-years")
@RequiredArgsConstructor
public class AcademicYearController {

    private final AcademicYearService academicYearService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<AcademicYearDto>>> list(Pageable pageable) {
        Page<AcademicYearDto> page = academicYearService.list(pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(page)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AcademicYearDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(academicYearService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ApiResponse<AcademicYearDto>> create(
            @Valid @RequestBody CreateAcademicYearRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Academic year created", academicYearService.create(request)));
    }

    @PatchMapping("/{id}/set-current")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ApiResponse<AcademicYearDto>> setCurrent(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Marked as current academic year",
                academicYearService.setCurrent(id)));
    }
}
