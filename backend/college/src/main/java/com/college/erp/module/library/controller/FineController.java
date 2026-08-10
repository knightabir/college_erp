package com.college.erp.module.library.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.college.erp.common.dto.ApiResponse;
import com.college.erp.module.library.dto.FineDto;
import com.college.erp.module.library.dto.PayFineRequest;
import com.college.erp.module.library.dto.WaiveFineRequest;
import com.college.erp.module.library.service.FineService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Not in 05-api-documentation.md's TOC — see FineService javadoc for waiver-authority scoping. */
@RestController
@RequestMapping("/api/v1/library/fines")
@RequiredArgsConstructor
public class FineController {

    private final FineService fineService;

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('CHIEF_LIBRARIAN','LIBRARY_ASSISTANT') "
            + "or #userId == principal.id")
    public ResponseEntity<ApiResponse<List<FineDto>>> listByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(fineService.listByUser(userId)));
    }

    @PatchMapping("/{id}/pay")
    @PreAuthorize("hasAnyRole('CHIEF_LIBRARIAN','LIBRARY_ASSISTANT')")
    public ResponseEntity<ApiResponse<FineDto>> pay(@PathVariable UUID id, @Valid @RequestBody PayFineRequest request) {
        return ResponseEntity.ok(ApiResponse.success(fineService.payFine(id, request.getAmount())));
    }

    @PatchMapping("/{id}/waive")
    @PreAuthorize("hasAnyRole('CHIEF_LIBRARIAN','LIBRARY_ASSISTANT')")
    public ResponseEntity<ApiResponse<FineDto>> waive(
            @PathVariable UUID id, @Valid @RequestBody WaiveFineRequest request) {
        return ResponseEntity.ok(ApiResponse.success(fineService.waive(id, request.getReason())));
    }
}
