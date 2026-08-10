package com.college.erp.notification.channel;

import org.springframework.stereotype.Component;

import com.college.erp.notification.NotificationRecord;

/**
 * 06-design-document.md §8.1: "InAppChannel.isEnabled() always returns true." The in-app inbox
 * row is actually written by NotificationService itself before any channel is consulted (step 1
 * of the delivery flow, unconditional) — this bean's send() is a deliberate no-op kept only for
 * symmetry with EmailChannel/SmsChannel and so NotificationService can iterate over channels
 * uniformly.
 */
@Component
public class InAppChannel implements NotificationChannel {

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void send(NotificationRecord record) {
        // No-op — see class javadoc.
    }
}
