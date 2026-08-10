package com.college.erp.module.communication.domain;

import java.time.Instant;
import java.util.UUID;

import com.college.erp.common.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 04-database-schema.md §15.2, 03-srs.md [COMM-002]. */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "helpdesk_tickets")
public class HelpdeskTicket extends BaseEntity {

    @Column(name = "raised_by_user_id", nullable = false)
    private UUID raisedByUserId;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private TicketCategory category;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private TicketPriority priority = TicketPriority.MEDIUM;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TicketStatus status = TicketStatus.OPEN;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolution", columnDefinition = "TEXT")
    private String resolution;

    /** [COMM-002]: OPEN → ASSIGNED. */
    public void assign(UUID assigneeUserId) {
        this.assignedTo = assigneeUserId;
        this.status = TicketStatus.ASSIGNED;
    }

    /** ASSIGNED/IN_PROGRESS → IN_PROGRESS (the assignee has started work). */
    public void startProgress() {
        this.status = TicketStatus.IN_PROGRESS;
    }

    /** → RESOLVED. */
    public void resolve(String resolution) {
        this.status = TicketStatus.RESOLVED;
        this.resolution = resolution;
        this.resolvedAt = Instant.now();
    }

    /** RESOLVED → CLOSED. */
    public void close() {
        this.status = TicketStatus.CLOSED;
    }
}
