package com.college.erp.module.hr.controller;

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
import com.college.erp.module.hr.dto.CreateDesignationRequest;
import com.college.erp.module.hr.dto.CreateStaffProfileRequest;
import com.college.erp.module.hr.dto.DesignationDto;
import com.college.erp.module.hr.dto.StaffProfileDto;
import com.college.erp.module.hr.service.DesignationService;
import com.college.erp.module.hr.service.StaffService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** 03-srs.md [HR-001]. Not fully enumerated in 05-api-documentation.md §10 — see StaffProfile javadoc. */
@RestController
@RequestMapping("/api/v1/hr/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;
    private final DesignationService designationService;

    @PostMapping("/designations")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_MANAGER')")
    public ResponseEntity<ApiResponse<DesignationDto>> createDesignation(
            @Valid @RequestBody CreateDesignationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(designationService.create(request)));
    }

    @GetMapping("/designations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<DesignationDto>>> listDesignations() {
        return ResponseEntity.ok(ApiResponse.success(designationService.list()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_MANAGER')")
    public ResponseEntity<ApiResponse<StaffProfileDto>> create(
            @Valid @RequestBody CreateStaffProfileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(staffService.create(request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_MANAGER','HOD','ADMIN')")
    public ResponseEntity<ApiResponse<StaffProfileDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(staffService.getById(id)));
    }

    @GetMapping("/by-department/{departmentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_MANAGER','HOD','ADMIN')")
    public ResponseEntity<ApiResponse<List<StaffProfileDto>>> listByDepartment(@PathVariable UUID departmentId) {
        return ResponseEntity.ok(ApiResponse.success(staffService.listByDepartment(departmentId)));
    }
}
