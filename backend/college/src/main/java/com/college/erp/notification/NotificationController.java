package com.college.erp.notification;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.college.erp.common.dto.ApiResponse;
import com.college.erp.common.dto.PageResponse;
import com.college.erp.notification.sse.SseEmitterRegistry;
import com.college.erp.security.jwt.UserPrincipal;

import lombok.RequiredArgsConstructor;

/** 05-api-documentation.md §12 "Notifications". */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final SseEmitterRegistry sseEmitterRegistry;

    @GetMapping("/stream")
    @PreAuthorize("isAuthenticated()")
    public SseEmitter stream(@AuthenticationPrincipal UserPrincipal principal) {
        return sseEmitterRegistry.create(principal.getId());
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<NotificationRecordDto>>> inbox(
            @AuthenticationPrincipal UserPrincipal principal, Pageable pageable) {
        Page<NotificationRecordDto> page = notificationService.inbox(principal.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(page)));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Long>> unreadCount(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.unreadCount(principal.getId())));
    }

    @PostMapping("/mark-all-read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Integer>> markAllRead(@AuthenticationPrincipal UserPrincipal principal) {
        int count = notificationService.markAllRead(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Marked " + count + " notification(s) as read", count));
    }
}
