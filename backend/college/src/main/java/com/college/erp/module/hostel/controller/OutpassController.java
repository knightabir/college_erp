package com.college.erp.module.hostel.controller;

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
import com.college.erp.module.hostel.dto.CreateOutpassRequest;
import com.college.erp.module.hostel.dto.OutpassRequestDto;
import com.college.erp.module.hostel.service.OutpassRequestService;
import com.college.erp.module.student.service.StudentService;
import com.college.erp.security.jwt.UserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** 04-database-schema.md §14.1. Not in 05-api-documentation.md — see the module's CLAUDE.md
 *  deviations note (no SRS/API basis, mirrors module.hr's leave-request shape). */
@RestController
@RequestMapping("/api/v1/hostel/outpass")
@RequiredArgsConstructor
public class OutpassController {

    private final OutpassRequestService outpassRequestService;
    private final StudentService studentService;

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<OutpassRequestDto>> apply(
            @Valid @RequestBody CreateOutpassRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        UUID studentId = studentService.getByUserId(principal.getId()).getId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(outpassRequestService.apply(request, studentId)));
    }

    @GetMapping("/my-requests")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<OutpassRequestDto>>> myRequests(
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID studentId = studentService.getByUserId(principal.getId()).getId();
        return ResponseEntity.ok(ApiResponse.success(outpassRequestService.listByStudent(studentId)));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HOSTEL_WARDEN')")
    public ResponseEntity<ApiResponse<List<OutpassRequestDto>>> pending() {
        return ResponseEntity.ok(ApiResponse.success(outpassRequestService.listPending()));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HOSTEL_WARDEN')")
    public ResponseEntity<ApiResponse<OutpassRequestDto>> approve(
            @PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(outpassRequestService.approve(id, principal.getId())));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HOSTEL_WARDEN')")
    public ResponseEntity<ApiResponse<OutpassRequestDto>> reject(
            @PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(outpassRequestService.reject(id, principal.getId())));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Void>> cancel(
            @PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        outpassRequestService.cancel(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Outpass request cancelled", null));
    }
}
