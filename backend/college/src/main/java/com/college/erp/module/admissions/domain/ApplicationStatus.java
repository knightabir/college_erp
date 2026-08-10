package com.college.erp.module.admissions.domain;

/**
 * INVENTED — see V015__create_admissions_tables.sql's header comment. Modeled as a linear
 * workflow: SUBMITTED → UNDER_REVIEW → SHORTLISTED → MERIT_LISTED → SEAT_ALLOTTED → ADMITTED,
 * with REJECTED/WITHDRAWN as terminal off-ramps from any non-terminal state.
 */
public enum ApplicationStatus {
    SUBMITTED,
    UNDER_REVIEW,
    SHORTLISTED,
    REJECTED,
    MERIT_LISTED,
    SEAT_ALLOTTED,
    ADMITTED,
    WITHDRAWN
}
