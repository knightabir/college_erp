package com.college.erp.module.placement.controller;

import java.util.List;
import java.util.UUID;

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
import com.college.erp.module.placement.dto.CompanyDto;
import com.college.erp.module.placement.dto.CreateCompanyRequest;
import com.college.erp.module.placement.dto.CreatePlacementDriveRequest;
import com.college.erp.module.placement.dto.CreatePlacementOfferRequest;
import com.college.erp.module.placement.dto.DriveApplicationDto;
import com.college.erp.module.placement.dto.InterviewRoundDto;
import com.college.erp.module.placement.dto.PlacementDriveDto;
import com.college.erp.module.placement.dto.PlacementOfferDto;
import com.college.erp.module.placement.dto.PlacementStatsDto;
import com.college.erp.module.placement.dto.RecordInterviewResultRequest;
import com.college.erp.module.placement.dto.ScheduleInterviewRoundRequest;
import com.college.erp.module.placement.dto.UpdateDriveApplicationStatusRequest;
import com.college.erp.module.placement.service.PlacementAnalyticsService;
import com.college.erp.module.placement.service.PlacementDriveService;
import com.college.erp.module.student.service.StudentService;
import com.college.erp.security.jwt.UserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * INVENTED module — see V016__create_placement_tables.sql's header comment. No
 * 05-api-documentation.md section exists to conform to — a single controller, matching
 * 08-system-architecture.md's file tree (one PlacementController for the whole module).
 */
@RestController
@RequestMapping("/api/v1/placement")
@RequiredArgsConstructor
public class PlacementController {

    private final PlacementDriveService placementDriveService;
    private final PlacementAnalyticsService placementAnalyticsService;
    private final StudentService studentService;

    @PostMapping("/companies")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLACEMENT_OFFICER')")
    public ResponseEntity<ApiResponse<CompanyDto>> createCompany(@Valid @RequestBody CreateCompanyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(placementDriveService.createCompany(request)));
    }

    @GetMapping("/companies")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<CompanyDto>>> listCompanies() {
        return ResponseEntity.ok(ApiResponse.success(placementDriveService.listCompanies()));
    }

    @PostMapping("/drives")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLACEMENT_OFFICER')")
    public ResponseEntity<ApiResponse<PlacementDriveDto>> createDrive(
            @Valid @RequestBody CreatePlacementDriveRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(placementDriveService.createDrive(request)));
    }

    @GetMapping("/drives")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<PlacementDriveDto>>> listDrivesByYear(
            @RequestParam UUID academicYearId) {
        return ResponseEntity.ok(ApiResponse.success(placementDriveService.listDrivesByYear(academicYearId)));
    }

    @GetMapping("/drives/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PlacementDriveDto>> getDrive(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(placementDriveService.getDriveById(id)));
    }

    @GetMapping("/drives/{id}/stats")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLACEMENT_OFFICER')")
    public ResponseEntity<ApiResponse<PlacementStatsDto>> getDriveStats(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(placementAnalyticsService.getStatsForDrive(id)));
    }

    @PostMapping("/drives/{id}/apply")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<DriveApplicationDto>> apply(
            @PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        UUID studentId = studentService.getByUserId(principal.getId()).getId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(placementDriveService.apply(id, studentId)));
    }

    @GetMapping("/drives/{id}/applications")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLACEMENT_OFFICER')")
    public ResponseEntity<ApiResponse<List<DriveApplicationDto>>> listApplicationsByDrive(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(placementDriveService.listApplicationsByDrive(id)));
    }

    @GetMapping("/my-applications")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<DriveApplicationDto>>> myApplications(
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID studentId = studentService.getByUserId(principal.getId()).getId();
        return ResponseEntity.ok(ApiResponse.success(placementDriveService.listApplicationsByStudent(studentId)));
    }

    @PatchMapping("/applications/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLACEMENT_OFFICER')")
    public ResponseEntity<ApiResponse<DriveApplicationDto>> updateApplicationStatus(
            @PathVariable UUID id, @Valid @RequestBody UpdateDriveApplicationStatusRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(placementDriveService.updateApplicationStatus(id, request.getStatus())));
    }

    @PostMapping("/applications/{id}/interview-rounds")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLACEMENT_OFFICER')")
    public ResponseEntity<ApiResponse<InterviewRoundDto>> scheduleInterviewRound(
            @PathVariable UUID id, @RequestBody ScheduleInterviewRoundRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(placementDriveService.scheduleInterviewRound(id, request)));
    }

    @GetMapping("/applications/{id}/interview-rounds")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<InterviewRoundDto>>> listInterviewRounds(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(placementDriveService.listInterviewRounds(id)));
    }

    @PatchMapping("/interview-rounds/{id}/result")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLACEMENT_OFFICER')")
    public ResponseEntity<ApiResponse<InterviewRoundDto>> recordInterviewResult(
            @PathVariable UUID id, @Valid @RequestBody RecordInterviewResultRequest request) {
        return ResponseEntity.ok(ApiResponse.success(placementDriveService.recordInterviewResult(id, request)));
    }

    @PostMapping("/applications/{id}/offer")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLACEMENT_OFFICER')")
    public ResponseEntity<ApiResponse<PlacementOfferDto>> makeOffer(
            @PathVariable UUID id, @Valid @RequestBody CreatePlacementOfferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(placementDriveService.makeOffer(id, request)));
    }

    @PatchMapping("/offers/{id}/accept")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<PlacementOfferDto>> acceptOffer(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(placementDriveService.respondToOffer(id, true)));
    }

    @PatchMapping("/offers/{id}/decline")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<PlacementOfferDto>> declineOffer(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(placementDriveService.respondToOffer(id, false)));
    }
}
