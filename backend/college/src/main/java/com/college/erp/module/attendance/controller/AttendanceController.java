package com.college.erp.module.attendance.controller;

import java.util.List;
import java.util.Map;
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
import com.college.erp.module.attendance.domain.AttendanceStatus;
import com.college.erp.module.attendance.dto.AttendanceMarkDto;
import com.college.erp.module.attendance.dto.AttendanceSessionDto;
import com.college.erp.module.attendance.dto.AttendanceSummaryDto;
import com.college.erp.module.attendance.dto.CondonationRequestDto;
import com.college.erp.module.attendance.dto.CreateCondonationRequest;
import com.college.erp.module.attendance.dto.CreateSessionRequest;
import com.college.erp.module.attendance.dto.DefaulterDto;
import com.college.erp.module.attendance.dto.ReviewCondonationRequest;
import com.college.erp.module.attendance.service.AttendanceCaptureService;
import com.college.erp.module.attendance.service.AttendanceService;
import com.college.erp.module.attendance.service.CondonationService;
import com.college.erp.security.jwt.UserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** 05-api-documentation.md §5 — single controller, matching the file tree's one AttendanceController.java. */
@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final AttendanceCaptureService attendanceCaptureService;
    private final CondonationService condonationService;

    @PostMapping("/sessions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HOD','FACULTY','LAB_ASSISTANT')")
    public ResponseEntity<ApiResponse<AttendanceSessionDto>> openSession(
            @Valid @RequestBody CreateSessionRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(attendanceService.openSession(request, principal.getId())));
    }

    @PostMapping("/sessions/{sessionId}/mark")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HOD','FACULTY','LAB_ASSISTANT')")
    public ResponseEntity<ApiResponse<List<AttendanceMarkDto>>> mark(
            @PathVariable UUID sessionId, @RequestBody Map<UUID, AttendanceStatus> statusByStudentId) {
        List<AttendanceMarkDto> marks = attendanceCaptureService.mark(sessionId, statusByStudentId);
        return ResponseEntity.ok(ApiResponse.success("Attendance marked successfully", marks));
    }

    @PostMapping("/sessions/{sessionId}/submit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> submitSession(
            @PathVariable UUID sessionId, @AuthenticationPrincipal UserPrincipal principal) {
        attendanceService.submitSession(sessionId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Attendance submitted", null));
    }

    @GetMapping("/summary/student/{studentId}/offering/{offeringId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HOD','FACULTY') "
            + "or @studentSecurityService.isOwnProfile(#studentId, principal)")
    public ResponseEntity<ApiResponse<AttendanceSummaryDto>> getSummary(
            @PathVariable UUID studentId, @PathVariable UUID offeringId) {
        return ResponseEntity.ok(ApiResponse.success(attendanceService.getSummary(studentId, offeringId)));
    }

    @GetMapping("/defaulters/{offeringId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HOD','FACULTY','EXAM_CELL')")
    public ResponseEntity<ApiResponse<List<DefaulterDto>>> getDefaulters(@PathVariable UUID offeringId) {
        return ResponseEntity.ok(ApiResponse.success(attendanceService.getDefaulters(offeringId)));
    }

    @PostMapping("/condonation")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<CondonationRequestDto>> applyCondonation(
            @Valid @RequestBody CreateCondonationRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        // The condoning student is the authenticated user's own student profile — CondonationService
        // resolves it from the principal, not from a client-supplied studentId.
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(condonationService.apply(principal.getId(), request)));
    }

    @PatchMapping("/condonation/{requestId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HOD','EXAM_CELL')")
    public ResponseEntity<ApiResponse<CondonationRequestDto>> reviewCondonation(
            @PathVariable UUID requestId, @Valid @RequestBody ReviewCondonationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                condonationService.review(requestId, request, principal.getId())));
    }
}
