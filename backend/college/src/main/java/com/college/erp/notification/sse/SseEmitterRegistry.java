package com.college.erp.notification.sse;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.extern.slf4j.Slf4j;

/**
 * 06-design-document.md §8.3: "one per logged-in user" — a second SSE connection from the same
 * user (e.g. opening a second tab) replaces the first rather than fanning out to both, matching
 * the doc's `Map<UUID, SseEmitter>` (not a multi-map).
 */
@Slf4j
@Component
public class SseEmitterRegistry {

    private static final long TIMEOUT_MILLIS = 30L * 60 * 1000; // 30 minutes

    private final Map<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter create(UUID userId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MILLIS);
        emitter.onCompletion(() -> remove(userId));
        emitter.onTimeout(() -> remove(userId));
        emitter.onError(ex -> remove(userId));
        emitters.put(userId, emitter);
        return emitter;
    }

    public SseEmitter get(UUID userId) {
        return emitters.get(userId);
    }

    public void remove(UUID userId) {
        emitters.remove(userId);
    }
}
