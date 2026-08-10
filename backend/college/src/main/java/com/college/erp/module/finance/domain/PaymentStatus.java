package com.college.erp.module.finance.domain;

/** Inferred — 04-database-schema.md §10.6 gives a default ('SUCCESS') but no value list. */
public enum PaymentStatus {
    SUCCESS,
    PENDING,
    FAILED,
    REFUNDED
}
