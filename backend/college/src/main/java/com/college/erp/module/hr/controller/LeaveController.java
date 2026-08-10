package com.college.erp.module.hr.controller;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.college.erp.common.dto.ApiResponse;
import com.college.erp.module.hr.dto.ApplyLeaveRequest;
import com.college.erp.module.hr.dto.CreateLeaveTypeRequest;
import com.college.erp.module.hr.dto.LeaveBalanceDto;
import com.college.erp.module.hr.dto.LeaveRequestDto;
import com.college.erp.module.hr.dto.LeaveTypeDto;
import com.college.erp.module.hr.dto.ReviewLeaveRequest;
import com.college.erp.module.hr.service.LeaveBalanceService;
import com.college.erp.module.hr.service.LeaveRequestService;
import com.college.erp.module.hr.service.LeaveTypeService;
import com.college.erp.security.jwt.UserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** 05-api-documentation.md §10 "HR & Leave". */
@RestController
@RequestMapping("/api/v1/hr/leave")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveTypeService leaveTypeService;
    private final LeaveBalanceService leaveBalanceService;
    private final LeaveRequestService leaveRequestService;

    @PostMapping("/types")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_MANAGER')")
    public ResponseEntity<ApiResponse<LeaveTypeDto>> createType(@Valid @RequestBody CreateLeaveTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(leaveTypeService.create(request)));
    }

    @GetMapping("/types")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<LeaveTypeDto>>> listTypes() {
        return ResponseEntity.ok(ApiResponse.success(leaveTypeService.list()));
    }

    @PostMapping("/balances/initialize")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_MANAGER')")
    public ResponseEntity<ApiResponse<Integer>> initializeBalances(@RequestParam int year) {
        int created = leaveBalanceService.initializeForYear(year);
        return ResponseEntity.ok(ApiResponse.success("Initialized " + created + " leave balance(s)", created));
    }

    @GetMapping("/balances")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<LeaveBalanceDto>>> myBalances(
            @AuthenticationPrincipal UserPrincipal principal, @RequestParam int year) {
        return ResponseEntity.ok(ApiResponse.success(leaveBalanceService.listByUserAndYear(principal.getId(), year)));
    }

    @PostMapping("/apply")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<LeaveRequestDto>> apply(
            @Valid @RequestBody ApplyLeaveRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(leaveRequestService.apply(request, principal.getId())));
    }

    @GetMapping("/my-requests")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<LeaveRequestDto>>> myRequests(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(leaveRequestService.listByUser(principal.getId())));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_MANAGER','HOD','ADMIN')")
    public ResponseEntity<ApiResponse<List<LeaveRequestDto>>> pending() {
        return ResponseEntity.ok(ApiResponse.success(leaveRequestService.listPending()));
    }

    @PatchMapping("/{requestId}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_MANAGER','HOD','ADMIN')")
    public ResponseEntity<ApiResponse<LeaveRequestDto>> approve(
            @PathVariable UUID requestId, @RequestBody ReviewLeaveRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                leaveRequestService.approve(requestId, request.getRemarks(), principal.getId())));
    }

    @PatchMapping("/{requestId}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_MANAGER','HOD','ADMIN')")
    public ResponseEntity<ApiResponse<LeaveRequestDto>> reject(
            @PathVariable UUID requestId, @RequestBody ReviewLeaveRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                leaveRequestService.reject(requestId, request.getRemarks(), principal.getId())));
    }

    @PatchMapping("/{requestId}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> cancel(
            @PathVariable UUID requestId, @AuthenticationPrincipal UserPrincipal principal) {
        leaveRequestService.cancel(requestId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Leave request cancelled", null));
    }
}
