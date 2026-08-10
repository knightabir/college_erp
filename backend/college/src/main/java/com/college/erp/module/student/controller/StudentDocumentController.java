package com.college.erp.module.student.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.college.erp.common.dto.ApiResponse;
import com.college.erp.module.student.dto.DocumentUploadDto;
import com.college.erp.module.student.dto.RejectDocumentRequest;
import com.college.erp.module.student.dto.StudentDocumentDto;
import com.college.erp.module.student.service.DocumentVerificationService;
import com.college.erp.security.jwt.UserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Not in 05-api-documentation.md's TOC — see DocumentVerificationService javadoc for scope. */
@RestController
@RequiredArgsConstructor
public class StudentDocumentController {

    private final DocumentVerificationService documentVerificationService;

    @GetMapping("/api/v1/students/{studentId}/documents")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HOD') "
            + "or @studentSecurityService.isOwnProfile(#studentId, principal)")
    public ResponseEntity<ApiResponse<List<StudentDocumentDto>>> list(@PathVariable UUID studentId) {
        return ResponseEntity.ok(ApiResponse.success(documentVerificationService.listByStudent(studentId)));
    }

    @PostMapping("/api/v1/students/{studentId}/documents")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HOD') "
            + "or @studentSecurityService.isOwnProfile(#studentId, principal)")
    public ResponseEntity<ApiResponse<StudentDocumentDto>> add(
            @PathVariable UUID studentId, @Valid @RequestBody DocumentUploadDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Document recorded", documentVerificationService.add(studentId, request)));
    }

    @PatchMapping("/api/v1/students/documents/{documentId}/verify")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HOD')")
    public ResponseEntity<ApiResponse<StudentDocumentDto>> verify(
            @PathVariable UUID documentId, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                documentVerificationService.verify(documentId, principal.getId())));
    }

    @PatchMapping("/api/v1/students/documents/{documentId}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HOD')")
    public ResponseEntity<ApiResponse<StudentDocumentDto>> reject(
            @PathVariable UUID documentId, @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody RejectDocumentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                documentVerificationService.reject(documentId, principal.getId(), request.getRemarks())));
    }
}
