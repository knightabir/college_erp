package com.college.erp.notification;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationRecordDto {

    private UUID id;
    private String title;
    private String body;
    private NotificationChannelType channel;
    private boolean read;
    private Instant readAt;
    private String referenceType;
    private String referenceId;
    private Instant createdAt;

    public static NotificationRecordDto from(NotificationRecord record) {
        return NotificationRecordDto.builder()
                .id(record.getId())
                .title(record.getTitle())
                .body(record.getBody())
                .channel(record.getChannel())
                .read(record.isRead())
                .readAt(record.getReadAt())
                .referenceType(record.getReferenceType())
                .referenceId(record.getReferenceId())
                .createdAt(record.getCreatedAt())
                .build();
    }
}
