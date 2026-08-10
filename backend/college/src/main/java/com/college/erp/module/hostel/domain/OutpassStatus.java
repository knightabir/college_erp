package com.college.erp.module.hostel.domain;

/** 04-database-schema.md §14.1 (values inferred, matching leave_requests' shape — see the
 *  module's CLAUDE.md deviations note). */
public enum OutpassStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED
}
