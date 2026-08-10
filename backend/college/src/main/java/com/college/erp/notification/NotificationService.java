package com.college.erp.notification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.notification.channel.EmailChannel;
import com.college.erp.notification.channel.NotificationChannel;
import com.college.erp.notification.channel.SmsChannel;
import com.college.erp.notification.sse.SseNotificationPusher;
import com.college.erp.notification.template.TemplateRenderer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 06-design-document.md §8.2 / 08-system-architecture.md §7. Entry point any module calls to
 * notify a user. send() is @Async — never blocks the caller's business transaction (same
 * REQUIRES_NEW-adjacent reasoning as AuditLogWriter, though here it's the whole method that's
 * async rather than just the DB write, since there's no caller-transaction state to preserve
 * beyond the NotificationRequest itself).
 *
 * Delivery flow — exactly the four steps in the design doc, in order, each independent of the
 * next: 1) persist NotificationRecord (the in-app inbox, always), 2) push over SSE if the user has
 * an open connection, 3) EmailChannel.send() if enabled, 4) SmsChannel.send() if enabled. Only one
 * row is ever written to notification_records per send() call — email/SMS delivery attempts
 * aren't separately persisted, matching the schema (a single `channel` column, not a join table).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRecordRepository notificationRecordRepository;
    private final NotificationTemplateRepository notificationTemplateRepository;
    private final TemplateRenderer templateRenderer;
    private final SseNotificationPusher sseNotificationPusher;
    private final EmailChannel emailChannel;
    private final SmsChannel smsChannel;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void send(NotificationRequest request) {
        try {
            String title = request.getTitle();
            String body = request.getBody();

            if (request.getTemplateKey() != null) {
                Optional<NotificationTemplate> template = notificationTemplateRepository
                        .findByTemplateKeyAndActiveTrueAndDeletedFalse(request.getTemplateKey());
                if (template.isPresent()) {
                    title = templateRenderer.render(
                            template.get().getSubject() != null ? template.get().getSubject() : title,
                            request.getVariables());
                    body = templateRenderer.render(template.get().getBodyText(), request.getVariables());
                } else {
                    log.warn("Notification template [{}] not found/active — falling back to raw title/body",
                            request.getTemplateKey());
                }
            }

            NotificationRecord record = NotificationRecord.builder()
                    .userId(request.getUserId())
                    .templateKey(request.getTemplateKey())
                    .title(title)
                    .body(body)
                    .channel(NotificationChannelType.IN_APP)
                    .sentAt(Instant.now())
                    .referenceType(request.getReferenceType())
                    .referenceId(request.getReferenceId())
                    .build();
            NotificationRecord saved = notificationRecordRepository.save(record);

            sseNotificationPusher.push(saved);

            sendIfEnabled(emailChannel, saved);
            sendIfEnabled(smsChannel, saved);
        } catch (Exception ex) {
            // A notification failure must never surface to the caller — it's already detached
            // (async) from the caller's transaction. Log loudly instead, same as AuditLogWriter.
            log.error("Failed to send notification to user [{}]: {}", request.getUserId(), request.getTitle(), ex);
        }
    }

    private void sendIfEnabled(NotificationChannel channel, NotificationRecord record) {
        if (!channel.isEnabled()) {
            return;
        }
        try {
            channel.send(record);
        } catch (Exception ex) {
            // Per the design doc: a channel failure must not roll back the steps before it.
            log.error("Notification channel [{}] failed for user [{}]", channel.getClass().getSimpleName(),
                    record.getUserId(), ex);
        }
    }

    @Transactional(readOnly = true)
    public Page<NotificationRecordDto> inbox(UUID userId, Pageable pageable) {
        return notificationRecordRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(NotificationRecordDto::from);
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        return notificationRecordRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public int markAllRead(UUID userId) {
        List<NotificationRecord> unread = notificationRecordRepository.findByUserIdAndReadFalse(userId);
        unread.forEach(NotificationRecord::markRead);
        notificationRecordRepository.saveAll(unread);
        return unread.size();
    }
}
