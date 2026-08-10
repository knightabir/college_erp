package com.college.erp.module.admin.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
import com.college.erp.common.dto.PageResponse;
import com.college.erp.module.admin.dto.AssignRoleRequest;
import com.college.erp.module.admin.dto.CreateUserRequest;
import com.college.erp.module.admin.dto.RoleAssignmentDto;
import com.college.erp.module.admin.dto.UserDto;
import com.college.erp.module.admin.service.UserManagementService;
import com.college.erp.security.jwt.UserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** 05-api-documentation.md §13 "User Management". */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class UserManagementController {

    private final UserManagementService userManagementService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ApiResponse<UserDto>> create(
            @Valid @RequestBody CreateUserRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(userManagementService.create(request, principal.getId())));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<UserDto>>> list(Pageable pageable) {
        Page<UserDto> page = userManagementService.list(pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(page)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ApiResponse<UserDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(userManagementService.getById(id)));
    }

    @PatchMapping("/{id}/lock")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ApiResponse<UserDto>> lock(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(userManagementService.lock(id)));
    }

    @PatchMapping("/{id}/unlock")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ApiResponse<UserDto>> unlock(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(userManagementService.unlock(id)));
    }

    @PostMapping("/{id}/roles")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ApiResponse<RoleAssignmentDto>> assignRole(
            @PathVariable UUID id, @Valid @RequestBody AssignRoleRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(userManagementService.assignRole(id, request, principal.getId())));
    }
}
