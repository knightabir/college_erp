package com.college.erp.module.library.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.college.erp.common.dto.ApiResponse;
import com.college.erp.module.library.dto.ReservationDto;
import com.college.erp.module.library.service.ReservationService;
import com.college.erp.security.jwt.UserPrincipal;

import lombok.RequiredArgsConstructor;

/** Not in 05-api-documentation.md's TOC — see 03-srs.md [LIB-007]. */
@RestController
@RequestMapping("/api/v1/library/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping("/titles/{titleId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReservationDto>> join(
            @PathVariable UUID titleId, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(reservationService.join(titleId, principal.getId())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> cancel(
            @PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        reservationService.cancel(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Reservation cancelled", null));
    }
}
