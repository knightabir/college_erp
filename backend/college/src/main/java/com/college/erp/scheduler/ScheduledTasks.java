package com.college.erp.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.college.erp.scheduler.jobs.JobTypes;

import lombok.RequiredArgsConstructor;

/**
 * 03-srs.md [SCHED-004] / 08-system-architecture.md §6.2. Cron triggers that enqueue a
 * BackgroundJob row for JobDispatcher's poller to pick up — these methods never run the job body
 * themselves, only INSERT.
 *
 * Two of the file tree's five named jobs are NOT wired here: MaintenanceRenewalAlertJob (AMC
 * renewal alerts, Mondays 09:00) has no `AnnualMaintenanceContract` table anywhere in
 * 04-database-schema.md — already documented as deferred in the asset module's CLAUDE.md notes —
 * and BackupVerificationJob has no backup mechanism specified in any of the eight planning docs.
 * Enqueuing either would only ever produce a job with no registered JobHandler; deferred until
 * their data sources exist.
 */
@Component
@RequiredArgsConstructor
public class ScheduledTasks {

    private final JobEnqueuer jobEnqueuer;

    @Scheduled(cron = "0 0 1 * * *")
    public void triggerAttendanceShortfallScan() {
        jobEnqueuer.enqueue(JobTypes.ATTENDANCE_SHORTFALL_SCAN, "{}");
    }

    @Scheduled(cron = "0 0 8 * * *")
    public void triggerFeeDueDateReminder() {
        jobEnqueuer.enqueue(JobTypes.FEE_DUE_DATE_REMINDER, "{}");
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void triggerLibraryOverdueScan() {
        jobEnqueuer.enqueue(JobTypes.LIBRARY_OVERDUE_SCAN, "{}");
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void triggerPermissionGrantExpiry() {
        jobEnqueuer.enqueue(JobTypes.PERMISSION_GRANT_EXPIRY, "{}");
    }
}
