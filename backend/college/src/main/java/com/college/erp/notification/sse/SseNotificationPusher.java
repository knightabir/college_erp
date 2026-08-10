package com.college.erp.notification.sse;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.college.erp.notification.NotificationRecord;
import com.college.erp.notification.NotificationRecordDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 08-system-architecture.md §7: "SseNotificationPusher.push() — if user has open SSE connection."
 * If there's no open connection, the notification is already safely in `notification_records`
 * (NotificationService wrote it first) — this is purely the "also push it live" step.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SseNotificationPusher {

    private final SseEmitterRegistry sseEmitterRegistry;

    public void push(NotificationRecord record) {
        SseEmitter emitter = sseEmitterRegistry.get(record.getUserId());
        if (emitter == null) {
            return;
        }
        try {
            emitter.send(SseEmitter.event()
                    .name("notification")
                    .data(NotificationRecordDto.from(record)));
        } catch (IOException | IllegalStateException ex) {
            // Client disconnected between the null-check and send() — not an error worth
            // propagating; the notification is already durably persisted.
            log.debug("SSE push failed for user [{}], removing stale emitter", record.getUserId(), ex);
            sseEmitterRegistry.remove(record.getUserId());
        }
    }
}
