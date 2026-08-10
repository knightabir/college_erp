package com.college.erp.module.finance.domain;

/** 04-database-schema.md §10.5. */
public enum InvoiceStatus {
    UNPAID,
    PARTIAL,
    PAID,
    OVERDUE,
    WAIVED,
    CANCELLED
}
