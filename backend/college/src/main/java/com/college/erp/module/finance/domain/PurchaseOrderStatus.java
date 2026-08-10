package com.college.erp.module.finance.domain;

/** 04-database-schema.md §10.9. */
public enum PurchaseOrderStatus {
    DRAFT,
    PENDING_APPROVAL,
    APPROVED,
    RECEIVED,
    CANCELLED
}
