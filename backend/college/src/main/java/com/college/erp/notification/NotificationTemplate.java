package com.college.erp.notification;

import com.college.erp.common.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 04-database-schema.md §16.1. */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "notification_templates")
public class NotificationTemplate extends BaseEntity {

    @Column(name = "template_key", nullable = false, unique = true, length = 100)
    private String templateKey;

    @Column(name = "subject", length = 300)
    private String subject;

    @Column(name = "body_text", nullable = false, columnDefinition = "TEXT")
    private String bodyText;

    @Column(name = "body_html", columnDefinition = "TEXT")
    private String bodyHtml;

    /** Comma-separated: EMAIL,SMS,IN_APP — see 04-database-schema.md §16.1's comment. */
    @Column(name = "channels", length = 100)
    private String channels;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
