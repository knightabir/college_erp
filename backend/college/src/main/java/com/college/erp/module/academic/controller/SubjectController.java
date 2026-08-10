package com.college.erp.module.academic.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.college.erp.common.dto.ApiResponse;
import com.college.erp.common.dto.PageResponse;
import com.college.erp.module.academic.dto.CreateSubjectOfferingRequest;
import com.college.erp.module.academic.dto.CreateSubjectRequest;
import com.college.erp.module.academic.dto.SubjectDto;
import com.college.erp.module.academic.dto.SubjectOfferingDto;
import com.college.erp.module.academic.service.SubjectService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Not in 05-api-documentation.md's TOC (only Departments/Courses/Batches are listed for
 *  academic) — endpoints below follow the same conventions (§1) by analogy. */
@RestController
@RequestMapping("/api/v1/academic/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<SubjectDto>>> list(Pageable pageable) {
        Page<SubjectDto> page = subjectService.list(pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(page)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SubjectDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(subjectService.getById(id)));
    }

    @GetMapping("/by-department/{departmentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<SubjectDto>>> listByDepartment(@PathVariable UUID departmentId) {
        return ResponseEntity.ok(ApiResponse.success(subjectService.listByDepartment(departmentId)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HOD')")
    public ResponseEntity<ApiResponse<SubjectDto>> create(@Valid @RequestBody CreateSubjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Subject created", subjectService.create(request)));
    }

    @PostMapping("/offerings")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HOD')")
    public ResponseEntity<ApiResponse<SubjectOfferingDto>> createOffering(
            @Valid @RequestBody CreateSubjectOfferingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Subject offering created", subjectService.createOffering(request)));
    }

    @GetMapping("/offerings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<SubjectOfferingDto>>> listOfferings(
            @RequestParam UUID batchId, @RequestParam UUID termId) {
        return ResponseEntity.ok(ApiResponse.success(subjectService.listOfferings(batchId, termId)));
    }
}
