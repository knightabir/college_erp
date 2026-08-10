package com.college.erp.module.communication.domain;

/** 04-database-schema.md §15.2, 03-srs.md [COMM-002]: "Status progresses through OPEN → ASSIGNED
 *  → IN_PROGRESS → RESOLVED → CLOSED." */
public enum TicketStatus {
    OPEN,
    ASSIGNED,
    IN_PROGRESS,
    RESOLVED,
    CLOSED
}
