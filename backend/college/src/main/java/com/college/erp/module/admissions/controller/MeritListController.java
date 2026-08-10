package com.college.erp.module.admissions.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.college.erp.common.dto.ApiResponse;
import com.college.erp.module.admissions.dto.GenerateMeritListRequest;
import com.college.erp.module.admissions.dto.MeritListDto;
import com.college.erp.module.admissions.dto.MeritListEntryDto;
import com.college.erp.module.admissions.dto.OfferSeatRequest;
import com.college.erp.module.admissions.dto.SeatAllotmentDto;
import com.college.erp.module.admissions.service.MeritProcessingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** INVENTED module — see V015's header comment / ApplicationController's javadoc. */
@RestController
@RequestMapping("/api/v1/admissions/merit-lists")
@RequiredArgsConstructor
public class MeritListController {

    private final MeritProcessingService meritProcessingService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','ADMISSIONS_OFFICER')")
    public ResponseEntity<ApiResponse<MeritListDto>> generate(@Valid @RequestBody GenerateMeritListRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(meritProcessingService.generate(request)));
    }

    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','ADMISSIONS_OFFICER')")
    public ResponseEntity<ApiResponse<MeritListDto>> publish(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(meritProcessingService.publish(id)));
    }

    @GetMapping("/{id}/entries")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<MeritListEntryDto>>> entries(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(meritProcessingService.getEntries(id)));
    }

    @PostMapping("/applications/{applicationId}/offer-seat")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','ADMISSIONS_OFFICER')")
    public ResponseEntity<ApiResponse<SeatAllotmentDto>> offerSeat(
            @PathVariable UUID applicationId, @Valid @RequestBody OfferSeatRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(meritProcessingService.offerSeat(applicationId, request)));
    }

    @PatchMapping("/seat-allotments/{seatAllotmentId}/accept")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SeatAllotmentDto>> acceptOffer(@PathVariable UUID seatAllotmentId) {
        return ResponseEntity.ok(ApiResponse.success(meritProcessingService.respondToOffer(seatAllotmentId, true)));
    }

    @PatchMapping("/seat-allotments/{seatAllotmentId}/decline")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SeatAllotmentDto>> declineOffer(@PathVariable UUID seatAllotmentId) {
        return ResponseEntity.ok(ApiResponse.success(meritProcessingService.respondToOffer(seatAllotmentId, false)));
    }
}
