package com.college.erp.module.asset.domain;

/**
 * 04-database-schema.md §12.4. Only OPEN and RESOLVED are ever set by this codebase (raise() and
 * resolve() — see MaintenanceService); ASSIGNED/IN_PROGRESS/CLOSED/CANCELLED exist in the schema's
 * documented value set but 05-api-documentation.md §9 doesn't expose any endpoint that transitions
 * a ticket through them, so they're modeled but unreachable until such an endpoint is specified.
 */
public enum TicketStatus {
    OPEN,
    ASSIGNED,
    IN_PROGRESS,
    RESOLVED,
    CLOSED,
    CANCELLED
}
