package com.college.erp.notification.channel;

import org.springframework.stereotype.Component;

import com.college.erp.config.ErpProperties;
import com.college.erp.notification.NotificationRecord;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 06-design-document.md §8.1: isEnabled() returns `erp.notification.sms.enabled` — "false in
 * air-gap profile". Same stub-send reasoning as EmailChannel: no SMS gateway is specified
 * anywhere in the docs, so send() logs the outbound attempt rather than integrating against a
 * guessed provider.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmsChannel implements NotificationChannel {

    private final ErpProperties erpProperties;

    @Override
    public boolean isEnabled() {
        return erpProperties.getNotification().isSmsEnabled();
    }

    @Override
    public void send(NotificationRecord record) {
        log.info("[SMS stub] Would SMS user [{}]: {}", record.getUserId(), record.getTitle());
    }
}
