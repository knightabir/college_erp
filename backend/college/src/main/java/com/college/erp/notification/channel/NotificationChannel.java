package com.college.erp.notification.channel;

import com.college.erp.notification.NotificationRecord;

/** 06-design-document.md §8.1. Three implementations: InAppChannel, EmailChannel, SmsChannel. */
public interface NotificationChannel {

    boolean isEnabled();

    void send(NotificationRecord record);
}
