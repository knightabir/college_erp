package com.college.erp.module.library.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.college.erp.common.dto.ApiResponse;
import com.college.erp.module.library.dto.CustodyLogDto;
import com.college.erp.module.library.dto.IssueBookDto;
import com.college.erp.module.library.dto.RenewBookRequest;
import com.college.erp.module.library.dto.ReturnBookDto;
import com.college.erp.module.library.service.CirculationService;
import com.college.erp.security.jwt.UserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** 05-api-documentation.md §8 "Circulation". */
@RestController
@RequestMapping("/api/v1/library/circulation")
@RequiredArgsConstructor
public class CirculationController {

    private final CirculationService circulationService;

    @PostMapping("/issue")
    @PreAuthorize("hasAnyRole('CHIEF_LIBRARIAN','LIBRARY_ASSISTANT')")
    public ResponseEntity<ApiResponse<CustodyLogDto>> issue(
            @Valid @RequestBody IssueBookDto request, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(circulationService.issue(request, principal.getId())));
    }

    @PostMapping("/return")
    @PreAuthorize("hasAnyRole('CHIEF_LIBRARIAN','LIBRARY_ASSISTANT')")
    public ResponseEntity<ApiResponse<CustodyLogDto>> returnBook(
            @Valid @RequestBody ReturnBookDto request, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(circulationService.returnBook(request, principal.getId())));
    }

    @PostMapping("/renew")
    @PreAuthorize("hasAnyRole('CHIEF_LIBRARIAN','LIBRARY_ASSISTANT')")
    public ResponseEntity<ApiResponse<CustodyLogDto>> renew(
            @Valid @RequestBody RenewBookRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(circulationService.renew(request, principal.getId())));
    }

    @GetMapping("/copies/{copyId}/custody-chain")
    @PreAuthorize("hasAnyRole('CHIEF_LIBRARIAN','LIBRARY_ASSISTANT')")
    public ResponseEntity<ApiResponse<List<CustodyLogDto>>> custodyChain(@PathVariable UUID copyId) {
        return ResponseEntity.ok(ApiResponse.success(circulationService.custodyChain(copyId)));
    }
}
