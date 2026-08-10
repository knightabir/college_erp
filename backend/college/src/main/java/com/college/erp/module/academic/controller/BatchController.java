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
import com.college.erp.module.academic.dto.BatchDto;
import com.college.erp.module.academic.dto.CreateBatchRequest;
import com.college.erp.module.academic.dto.CreateSectionRequest;
import com.college.erp.module.academic.dto.SectionDto;
import com.college.erp.module.academic.service.BatchService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** 05-api-documentation.md §3 "Batches". */
@RestController
@RequestMapping("/api/v1/academic/batches")
@RequiredArgsConstructor
public class BatchController {

    private final BatchService batchService;

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BatchDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(batchService.getById(id)));
    }

    @GetMapping("/by-course/{courseId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<BatchDto>>> listByCourse(@PathVariable UUID courseId) {
        return ResponseEntity.ok(ApiResponse.success(batchService.listByCourse(courseId)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ApiResponse<BatchDto>> create(@Valid @RequestBody CreateBatchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Batch created", batchService.create(request)));
    }

    @PostMapping("/{id}/promote-term")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> promoteTerm(@PathVariable UUID id) {
        batchService.promoteTerm(id);
        return ResponseEntity.ok(ApiResponse.success("Batch promoted to next term", null));
    }

    @GetMapping("/{id}/sections")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<SectionDto>>> listSections(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(batchService.listSections(id)));
    }

    @PostMapping("/{id}/sections")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HOD')")
    public ResponseEntity<ApiResponse<SectionDto>> addSection(
            @PathVariable UUID id, @Valid @RequestBody CreateSectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Section created", batchService.addSection(id, request)));
    }
}
