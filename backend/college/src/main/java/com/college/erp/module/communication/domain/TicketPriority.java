package com.college.erp.module.communication.domain;

/** 04-database-schema.md §15.2's DDL comment doesn't enumerate priority values (unlike
 *  category/status) — inferred from 05-api-documentation.md §11's "priority": "HIGH" example and
 *  matching module.asset's maintenance_tickets shape. */
public enum TicketPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
