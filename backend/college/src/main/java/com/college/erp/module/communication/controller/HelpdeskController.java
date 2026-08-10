package com.college.erp.module.communication.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.college.erp.common.dto.ApiResponse;
import com.college.erp.common.dto.PageResponse;
import com.college.erp.module.communication.domain.TicketStatus;
import com.college.erp.module.communication.dto.AddCommentRequest;
import com.college.erp.module.communication.dto.AssignTicketRequest;
import com.college.erp.module.communication.dto.HelpdeskTicketDto;
import com.college.erp.module.communication.dto.RaiseTicketRequest;
import com.college.erp.module.communication.dto.ResolveTicketRequest;
import com.college.erp.module.communication.dto.TicketCommentDto;
import com.college.erp.module.communication.service.HelpdeskService;
import com.college.erp.security.jwt.UserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 05-api-documentation.md §11 "Communication" (helpdesk half). Only POST /helpdesk and
 * GET /helpdesk/my-tickets are in the API doc's TOC; assign/start-progress/resolve/close/comment
 * follow directly from [COMM-002]'s described lifecycle — same "SRS describes more than the API
 * doc's TOC enumerates" situation as library's FineController.
 */
@RestController
@RequestMapping("/api/v1/helpdesk")
@RequiredArgsConstructor
public class HelpdeskController {

    private final HelpdeskService helpdeskService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<HelpdeskTicketDto>> raise(
            @Valid @RequestBody RaiseTicketRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(helpdeskService.raise(request, principal.getId())));
    }

    @GetMapping("/my-tickets")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<HelpdeskTicketDto>>> myTickets(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) TicketStatus status, Pageable pageable) {
        Page<HelpdeskTicketDto> page = helpdeskService.myTickets(principal.getId(), status, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(page)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<HelpdeskTicketDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(helpdeskService.getById(id)));
    }

    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','IT_ADMIN')")
    public ResponseEntity<ApiResponse<HelpdeskTicketDto>> assign(
            @PathVariable UUID id, @Valid @RequestBody AssignTicketRequest request) {
        return ResponseEntity.ok(ApiResponse.success(helpdeskService.assign(id, request.getAssigneeUserId())));
    }

    @PatchMapping("/{id}/start-progress")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','IT_ADMIN')")
    public ResponseEntity<ApiResponse<HelpdeskTicketDto>> startProgress(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(helpdeskService.startProgress(id)));
    }

    @PatchMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','IT_ADMIN')")
    public ResponseEntity<ApiResponse<HelpdeskTicketDto>> resolve(
            @PathVariable UUID id, @Valid @RequestBody ResolveTicketRequest request) {
        return ResponseEntity.ok(ApiResponse.success(helpdeskService.resolve(id, request.getResolution())));
    }

    @PatchMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','IT_ADMIN')")
    public ResponseEntity<ApiResponse<HelpdeskTicketDto>> close(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(helpdeskService.close(id)));
    }

    @PostMapping("/{id}/comments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TicketCommentDto>> addComment(
            @PathVariable UUID id, @Valid @RequestBody AddCommentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(helpdeskService.addComment(id, request, principal.getId())));
    }

    @GetMapping("/{id}/comments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<TicketCommentDto>>> listComments(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(helpdeskService.listComments(id)));
    }
}
