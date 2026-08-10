package com.college.erp.scheduler.jobs;

/** job_type string constants — must match a JobHandler.jobType() exactly. */
public final class JobTypes {

    public static final String ATTENDANCE_SHORTFALL_SCAN = "ATTENDANCE_SHORTFALL_SCAN";
    public static final String FEE_DUE_DATE_REMINDER = "FEE_DUE_DATE_REMINDER";
    public static final String LIBRARY_OVERDUE_SCAN = "LIBRARY_OVERDUE_SCAN";
    public static final String PERMISSION_GRANT_EXPIRY = "PERMISSION_GRANT_EXPIRY";

    private JobTypes() {
    }
}
