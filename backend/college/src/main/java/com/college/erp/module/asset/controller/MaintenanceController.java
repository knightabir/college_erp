package com.college.erp.module.asset.controller;

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
import com.college.erp.module.asset.dto.MaintenanceTicketDto;
import com.college.erp.module.asset.dto.RaiseTicketRequest;
import com.college.erp.module.asset.dto.ResolveTicketRequest;
import com.college.erp.module.asset.service.MaintenanceService;
import com.college.erp.security.jwt.UserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** 05-api-documentation.md §9 "Asset Management" — maintenance ticket endpoints. */
@RestController
@RequestMapping("/api/v1/assets")
@RequiredArgsConstructor
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    @PostMapping("/{id}/maintenance-tickets")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MaintenanceTicketDto>> raise(
            @PathVariable UUID id, @Valid @RequestBody RaiseTicketRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(maintenanceService.raise(id, request, principal.getId())));
    }

    @GetMapping("/{id}/maintenance-tickets")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','ASSET_MANAGER','MAINTENANCE_STAFF')")
    public ResponseEntity<ApiResponse<List<MaintenanceTicketDto>>> listByAsset(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(maintenanceService.listByAsset(id)));
    }

    @PatchMapping("/tickets/{ticketId}/resolve")
    @PreAuthorize("hasAnyRole('ASSET_MANAGER','MAINTENANCE_STAFF')")
    public ResponseEntity<ApiResponse<MaintenanceTicketDto>> resolve(
            @PathVariable UUID ticketId, @Valid @RequestBody ResolveTicketRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                maintenanceService.resolve(ticketId, request, principal.getId())));
    }
}
