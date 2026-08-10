package com.college.erp.module.communication.domain;

import java.util.UUID;

import com.college.erp.common.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 04-database-schema.md §15.2, 03-srs.md [COMM-002]: "Comments may be added by the ticket owner
 *  or the assigned staff." */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ticket_comments")
public class TicketComment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private HelpdeskTicket ticket;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "comment", nullable = false, columnDefinition = "TEXT")
    private String comment;
}
