package com.college.erp.notification;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 04-database-schema.md §16.2. The persisted in-app inbox entry — written once per
 * NotificationService.send() call regardless of how many channels ultimately deliver it (see
 * NotificationService javadoc). No blanket @Setter: only markRead() ever mutates a row after
 * insert, matching the "append-only except one narrow transition" shape used elsewhere
 * (MaintenanceTicket.resolve(), LibraryCustodyLog.recordReturn()).
 */
@Getter
@Entity
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "notification_records",
    indexes = {
        @Index(name = "idx_notification_user", columnList = "user_id")
    }
)
public class NotificationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "template_key", length = 100, updatable = false)
    private String templateKey;

    @Column(name = "title", nullable = false, length = 300, updatable = false)
    private String title;

    @Column(name = "body", nullable = false, updatable = false, columnDefinition = "TEXT")
    private String body;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20, updatable = false)
    private NotificationChannelType channel = NotificationChannelType.IN_APP;

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "sent_at", updatable = false)
    private Instant sentAt;

    @Column(name = "reference_type", length = 50, updatable = false)
    private String referenceType;

    @Column(name = "reference_id", length = 36, updatable = false)
    private String referenceId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** The one allowed mutation — see class javadoc. */
    public void markRead() {
        this.read = true;
        this.readAt = Instant.now();
    }
}
