package com.college.erp.scheduler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 03-srs.md [SCHED-002]/[SCHED-003]. Named to match the kernel's other domain-behavior tests. */
class BackgroundJobTest {

    @Test
    @DisplayName("markRunning() — status RUNNING, attempts incremented, startedAt set")
    void markRunning_incrementsAttemptsAndSetsStatus() {
        // ARRANGE
        BackgroundJob job = BackgroundJob.builder().jobType("TEST").attempts(0).build();

        // ACT
        job.markRunning();

        // ASSERT
        assertThat(job.getStatus()).isEqualTo(JobStatus.RUNNING);
        assertThat(job.getAttempts()).isEqualTo(1);
        assertThat(job.getStartedAt()).isNotNull();
    }

    @Test
    @DisplayName("markDone() — status DONE, completedAt set")
    void markDone_setsStatusAndCompletedAt() {
        // ARRANGE
        BackgroundJob job = BackgroundJob.builder().jobType("TEST").build();
        job.markRunning();

        // ACT
        job.markDone();

        // ASSERT
        assertThat(job.getStatus()).isEqualTo(JobStatus.DONE);
        assertThat(job.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("markFailedOrRetry() — under maxAttempts: back to PENDING with exponential backoff "
            + "(03-srs.md [SCHED-003]: 2^attempts × 30s)")
    void markFailedOrRetry_underMaxAttempts_reschedulesWithBackoff() {
        // ARRANGE
        BackgroundJob job = BackgroundJob.builder().jobType("TEST").maxAttempts(3).attempts(0).build();
        job.markRunning(); // attempts -> 1

        // ACT
        job.markFailedOrRetry("boom");

        // ASSERT — attempt 1 failure retries in 2^1 * 30 = 60s
        assertThat(job.getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(job.getLastError()).isEqualTo("boom");
        assertThat(job.getScheduledFor()).isAfter(job.getStartedAt().plusSeconds(59));
        assertThat(job.getScheduledFor()).isBefore(job.getStartedAt().plusSeconds(61));
    }

    @Test
    @DisplayName("markFailedOrRetry() — at maxAttempts: FAILED, not rescheduled")
    void markFailedOrRetry_atMaxAttempts_marksFailed() {
        // ARRANGE
        BackgroundJob job = BackgroundJob.builder().jobType("TEST").maxAttempts(1).attempts(0).build();
        job.markRunning(); // attempts -> 1 == maxAttempts

        // ACT
        job.markFailedOrRetry("boom");

        // ASSERT
        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getCompletedAt()).isNotNull();
    }
}
