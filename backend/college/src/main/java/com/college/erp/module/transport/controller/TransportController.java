package com.college.erp.module.transport.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.college.erp.common.dto.ApiResponse;
import com.college.erp.module.transport.dto.AssignRouteRequest;
import com.college.erp.module.transport.dto.CreateRouteRequest;
import com.college.erp.module.transport.dto.CreateVehicleRequest;
import com.college.erp.module.transport.dto.RouteDto;
import com.college.erp.module.transport.dto.StudentRouteMappingDto;
import com.college.erp.module.transport.dto.VehicleDto;
import com.college.erp.module.transport.service.RouteService;
import com.college.erp.module.transport.service.StudentRouteMappingService;
import com.college.erp.module.transport.service.VehicleService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** 04-database-schema.md §14.2. Not in 05-api-documentation.md — see the module's CLAUDE.md
 *  deviations note (no SRS/API basis). */
@RestController
@RequestMapping("/api/v1/transport")
@RequiredArgsConstructor
public class TransportController {

    private final RouteService routeService;
    private final VehicleService vehicleService;
    private final StudentRouteMappingService studentRouteMappingService;

    @PostMapping("/routes")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','TRANSPORT_MANAGER')")
    public ResponseEntity<ApiResponse<RouteDto>> createRoute(@Valid @RequestBody CreateRouteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(routeService.create(request)));
    }

    @GetMapping("/routes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<RouteDto>>> listRoutes() {
        return ResponseEntity.ok(ApiResponse.success(routeService.listActive()));
    }

    @PostMapping("/vehicles")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','TRANSPORT_MANAGER')")
    public ResponseEntity<ApiResponse<VehicleDto>> createVehicle(@Valid @RequestBody CreateVehicleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(vehicleService.create(request)));
    }

    @GetMapping("/vehicles")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','TRANSPORT_MANAGER')")
    public ResponseEntity<ApiResponse<List<VehicleDto>>> listVehicles() {
        return ResponseEntity.ok(ApiResponse.success(vehicleService.listActive()));
    }

    @PostMapping("/route-mappings")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','TRANSPORT_MANAGER')")
    public ResponseEntity<ApiResponse<StudentRouteMappingDto>> assign(
            @Valid @RequestBody AssignRouteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(studentRouteMappingService.assign(request)));
    }

    @PatchMapping("/route-mappings/{id}/deactivate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','TRANSPORT_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable UUID id) {
        studentRouteMappingService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success("Route mapping deactivated", null));
    }

    @GetMapping("/routes/{routeId}/students")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','TRANSPORT_MANAGER')")
    public ResponseEntity<ApiResponse<List<StudentRouteMappingDto>>> listByRoute(@PathVariable UUID routeId) {
        return ResponseEntity.ok(ApiResponse.success(studentRouteMappingService.listByRoute(routeId)));
    }
}
