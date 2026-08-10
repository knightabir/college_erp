package com.college.erp.notification;

import java.util.Map;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

/**
 * What any module passes to NotificationService.send(...). Either supply templateKey + variables
 * (rendered via TemplateRenderer against a NotificationTemplate row) or a raw title/body directly
 * — templateKey wins if both are given and the template is found/active.
 */
@Getter
@Builder
public class NotificationRequest {

    private UUID userId;
    private String templateKey;
    private Map<String, String> variables;
    private String title;
    private String body;
    private String referenceType;
    private String referenceId;
}
