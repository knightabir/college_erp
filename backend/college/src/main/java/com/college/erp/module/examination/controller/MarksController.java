package com.college.erp.module.examination.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.college.erp.common.dto.ApiResponse;
import com.college.erp.module.examination.dto.CompleteRevaluationRequest;
import com.college.erp.module.examination.dto.CreateRevaluationRequest;
import com.college.erp.module.examination.dto.EnterMarksRequest;
import com.college.erp.module.examination.dto.MarksEntryDto;
import com.college.erp.module.examination.dto.RevaluationRequestDto;
import com.college.erp.module.examination.service.MarksEntryService;
import com.college.erp.module.examination.service.RevaluationService;
import com.college.erp.security.jwt.UserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 05-api-documentation.md §6 POST/PATCH /exams/marks/*, POST /exams/revaluation. Revaluation
 * lives here too (no dedicated RevaluationController in the file tree) — it's a marks-entry
 * concern (03-srs.md [EXAM-007]).
 */
@RestController
@RequestMapping("/api/v1/exams")
@RequiredArgsConstructor
public class MarksController {

    private final MarksEntryService marksEntryService;
    private final RevaluationService revaluationService;

    @PostMapping("/marks")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','EXAM_CELL') or "
            + "(hasRole('FACULTY') and @timetableService.isFacultyAssignedToOffering(principal.id, #request.offeringId))")
    public ResponseEntity<ApiResponse<MarksEntryDto>> enter(
            @Valid @RequestBody EnterMarksRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(marksEntryService.enter(request, principal.getId())));
    }

    @PatchMapping("/marks/{marksEntryId}/submit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MarksEntryDto>> submit(
            @PathVariable UUID marksEntryId, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(marksEntryService.submit(marksEntryId, principal.getId())));
    }

    @PatchMapping("/marks/{examId}/moderate")
    @PreAuthorize("hasAnyRole('HOD','EXAM_CELL','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Integer>> moderate(
            @PathVariable UUID examId, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(marksEntryService.moderateExam(examId, principal.getId())));
    }

    @PostMapping("/revaluation")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<RevaluationRequestDto>> applyRevaluation(
            @Valid @RequestBody CreateRevaluationRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(revaluationService.apply(principal.getId(), request)));
    }

    @PatchMapping("/revaluation/{requestId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','EXAM_CELL')")
    public ResponseEntity<ApiResponse<RevaluationRequestDto>> completeRevaluation(
            @PathVariable UUID requestId, @Valid @RequestBody CompleteRevaluationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                revaluationService.complete(requestId, request, principal.getId())));
    }

    @GetMapping("/revaluation/student/{studentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','EXAM_CELL') "
            + "or @studentSecurityService.isOwnProfile(#studentId, principal)")
    public ResponseEntity<ApiResponse<List<RevaluationRequestDto>>> listRevaluationsForStudent(
            @PathVariable UUID studentId) {
        return ResponseEntity.ok(ApiResponse.success(revaluationService.listByStudent(studentId)));
    }
}
