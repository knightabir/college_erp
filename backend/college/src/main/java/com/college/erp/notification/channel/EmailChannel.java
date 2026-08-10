package com.college.erp.notification.channel;

import org.springframework.stereotype.Component;

import com.college.erp.config.ErpProperties;
import com.college.erp.notification.NotificationRecord;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 06-design-document.md §8.1: isEnabled() returns `erp.notification.email.enabled` (false by
 * default — ADR-001 air-gap-first). No SMTP client (`spring-boot-starter-mail`/`JavaMailSender`)
 * is wired up — none of the eight planning docs specify SMTP host/credentials config, so there is
 * nothing concrete to integrate against yet. send() logs the outbound attempt at INFO instead of
 * silently dropping it, the same "detected but only logged, documented as a stand-in" pattern
 * used for [ATT-007]/[LIB-007] before this module existed. Wire a real JavaMailSender here before
 * turning erp.notification.email.enabled on for anything beyond local testing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailChannel implements NotificationChannel {

    private final ErpProperties erpProperties;

    @Override
    public boolean isEnabled() {
        return erpProperties.getNotification().isEmailEnabled();
    }

    @Override
    public void send(NotificationRecord record) {
        log.info("[EMAIL stub] Would email user [{}]: {} — {}", record.getUserId(), record.getTitle(),
                record.getBody());
    }
}
