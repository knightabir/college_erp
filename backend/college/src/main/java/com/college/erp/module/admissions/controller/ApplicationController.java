package com.college.erp.module.admissions.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.college.erp.common.dto.ApiResponse;
import com.college.erp.common.dto.PageResponse;
import com.college.erp.module.admissions.domain.ApplicationStatus;
import com.college.erp.module.admissions.dto.ApplicationDocumentDto;
import com.college.erp.module.admissions.dto.ApplicationDto;
import com.college.erp.module.admissions.dto.ApplicationWorkflowStepDto;
import com.college.erp.module.admissions.dto.ConvertToStudentRequest;
import com.college.erp.module.admissions.dto.CreateApplicationRequest;
import com.college.erp.module.admissions.dto.CreateLateralEntryDetailsRequest;
import com.college.erp.module.admissions.dto.LateralEntryApplicationDto;
import com.college.erp.module.admissions.dto.ReviewApplicationRequest;
import com.college.erp.module.admissions.dto.UploadApplicationDocumentRequest;
import com.college.erp.module.admissions.service.ApplicationWorkflowService;
import com.college.erp.module.admissions.service.EnrollmentConversionService;
import com.college.erp.module.student.dto.StudentDto;
import com.college.erp.security.jwt.UserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * INVENTED module — see V015__create_admissions_tables.sql's header comment. No
 * 05-api-documentation.md section exists to conform to; endpoint paths/auth roles were chosen for
 * consistency with the rest of the API, not transcribed from a spec (same situation as
 * module.hostel/module.transport).
 */
@RestController
@RequestMapping("/api/v1/admissions/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationWorkflowService applicationWorkflowService;
    private final EnrollmentConversionService enrollmentConversionService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ApplicationDto>> apply(@Valid @RequestBody CreateApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(applicationWorkflowService.apply(request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','ADMISSIONS_OFFICER')")
    public ResponseEntity<ApiResponse<ApplicationDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(applicationWorkflowService.getById(id)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','ADMISSIONS_OFFICER')")
    public ResponseEntity<ApiResponse<PageResponse<ApplicationDto>>> listByCourseAndYear(
            @RequestParam UUID courseId, @RequestParam UUID academicYearId, Pageable pageable) {
        Page<ApplicationDto> page = applicationWorkflowService.listByCourseAndYear(courseId, academicYearId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(page)));
    }

    @GetMapping("/by-status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','ADMISSIONS_OFFICER')")
    public ResponseEntity<ApiResponse<PageResponse<ApplicationDto>>> listByStatus(
            @RequestParam ApplicationStatus status, Pageable pageable) {
        Page<ApplicationDto> page = applicationWorkflowService.listByStatus(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(page)));
    }

    @PatchMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','ADMISSIONS_OFFICER')")
    public ResponseEntity<ApiResponse<ApplicationDto>> review(
            @PathVariable UUID id, @Valid @RequestBody ReviewApplicationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(
                ApiResponse.success(applicationWorkflowService.review(id, request, principal.getId())));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','ADMISSIONS_OFFICER')")
    public ResponseEntity<ApiResponse<List<ApplicationWorkflowStepDto>>> history(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(applicationWorkflowService.getHistory(id)));
    }

    @PostMapping("/{id}/documents")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ApplicationDocumentDto>> uploadDocument(
            @PathVariable UUID id, @Valid @RequestBody UploadApplicationDocumentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(applicationWorkflowService.uploadDocument(id, request)));
    }

    @GetMapping("/{id}/documents")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','ADMISSIONS_OFFICER')")
    public ResponseEntity<ApiResponse<List<ApplicationDocumentDto>>> listDocuments(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(applicationWorkflowService.listDocuments(id)));
    }

    @PatchMapping("/documents/{documentId}/verify")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','ADMISSIONS_OFFICER')")
    public ResponseEntity<ApiResponse<ApplicationDocumentDto>> verifyDocument(
            @PathVariable UUID documentId, @RequestParam boolean approve,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                applicationWorkflowService.verifyDocument(documentId, principal.getId(), approve)));
    }

    @PostMapping("/{id}/lateral-entry")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','ADMISSIONS_OFFICER')")
    public ResponseEntity<ApiResponse<LateralEntryApplicationDto>> recordLateralEntryDetails(
            @PathVariable UUID id, @RequestBody CreateLateralEntryDetailsRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(applicationWorkflowService.recordLateralEntryDetails(id, request)));
    }

    @PostMapping("/{id}/convert-to-student")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','ADMISSIONS_OFFICER')")
    public ResponseEntity<ApiResponse<StudentDto>> convertToStudent(
            @PathVariable UUID id, @Valid @RequestBody ConvertToStudentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(enrollmentConversionService.convert(id, request, principal.getId())));
    }
}
