package com.college.erp.module.hostel.controller;

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
import com.college.erp.module.hostel.dto.AllocateRoomRequest;
import com.college.erp.module.hostel.dto.CreateHostelBlockRequest;
import com.college.erp.module.hostel.dto.CreateRoomRequest;
import com.college.erp.module.hostel.dto.HostelBlockDto;
import com.college.erp.module.hostel.dto.RoomAllocationDto;
import com.college.erp.module.hostel.dto.RoomDto;
import com.college.erp.module.hostel.service.HostelBlockService;
import com.college.erp.module.hostel.service.RoomAllocationService;
import com.college.erp.module.hostel.service.RoomService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** 04-database-schema.md §14.1. Not in 05-api-documentation.md — see the module's CLAUDE.md
 *  deviations note (no SRS/API basis, built from the schema alone). */
@RestController
@RequestMapping("/api/v1/hostel")
@RequiredArgsConstructor
public class HostelController {

    private final HostelBlockService hostelBlockService;
    private final RoomService roomService;
    private final RoomAllocationService roomAllocationService;

    @PostMapping("/blocks")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HOSTEL_WARDEN')")
    public ResponseEntity<ApiResponse<HostelBlockDto>> createBlock(
            @Valid @RequestBody CreateHostelBlockRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(hostelBlockService.create(request)));
    }

    @GetMapping("/blocks")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<HostelBlockDto>>> listBlocks() {
        return ResponseEntity.ok(ApiResponse.success(hostelBlockService.list()));
    }

    @PostMapping("/blocks/{blockId}/rooms")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HOSTEL_WARDEN')")
    public ResponseEntity<ApiResponse<RoomDto>> createRoom(
            @PathVariable UUID blockId, @Valid @RequestBody CreateRoomRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(roomService.create(blockId, request)));
    }

    @GetMapping("/blocks/{blockId}/rooms")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<RoomDto>>> listRooms(@PathVariable UUID blockId) {
        return ResponseEntity.ok(ApiResponse.success(roomService.listByBlock(blockId)));
    }

    @PostMapping("/allocations")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HOSTEL_WARDEN')")
    public ResponseEntity<ApiResponse<RoomAllocationDto>> allocate(
            @Valid @RequestBody AllocateRoomRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(roomAllocationService.allocate(request)));
    }

    @PatchMapping("/allocations/{id}/vacate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HOSTEL_WARDEN')")
    public ResponseEntity<ApiResponse<RoomAllocationDto>> vacate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(roomAllocationService.vacate(id)));
    }

    @GetMapping("/rooms/{roomId}/allocations")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HOSTEL_WARDEN')")
    public ResponseEntity<ApiResponse<List<RoomAllocationDto>>> listAllocations(@PathVariable UUID roomId) {
        return ResponseEntity.ok(ApiResponse.success(roomAllocationService.listActiveByRoom(roomId)));
    }
}
