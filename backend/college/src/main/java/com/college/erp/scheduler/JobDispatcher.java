package com.college.erp.scheduler;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

/**
 * 03-srs.md [SCHED-002]/[SCHED-003], 06-design-document.md §7.2. Polls every 30 seconds, claims up
 * to 10 due PENDING jobs with `FOR UPDATE SKIP LOCKED` (safe across multiple app instances —
 * [NFR-SCALE-002]), and routes each to the JobHandler bean whose jobType() matches.
 */
@Slf4j
@Service
public class JobDispatcher {

    private static final int BATCH_SIZE = 10;
    private static final int CLEANUP_AFTER_DAYS = 7;

    private final BackgroundJobRepository backgroundJobRepository;
    private final Map<String, JobHandler> handlersByType;

    public JobDispatcher(BackgroundJobRepository backgroundJobRepository, List<JobHandler> handlers) {
        this.backgroundJobRepository = backgroundJobRepository;
        this.handlersByType = handlers.stream().collect(Collectors.toMap(JobHandler::jobType, Function.identity()));
    }

    @Scheduled(fixedDelay = 30_000)
    @Transactional
    public void pollAndDispatch() {
        List<BackgroundJob> due = backgroundJobRepository.findDuePendingForUpdateSkipLocked(BATCH_SIZE);
        for (BackgroundJob job : due) {
            dispatch(job);
        }
    }

    private void dispatch(BackgroundJob job) {
        job.markRunning();

        JobHandler handler = handlersByType.get(job.getJobType());
        if (handler == null) {
            job.markFailedOrRetry("No JobHandler registered for job_type '" + job.getJobType() + "'");
            backgroundJobRepository.save(job);
            log.error("No JobHandler for job_type [{}] (job {})", job.getJobType(), job.getId());
            return;
        }

        try {
            handler.handle(job.getPayload());
            job.markDone();
            log.info("Job [{}] ({}) completed", job.getId(), job.getJobType());
        } catch (Exception ex) {
            job.markFailedOrRetry(ex.getMessage());
            if (job.getStatus() == JobStatus.FAILED) {
                log.error("Job [{}] ({}) failed permanently after {} attempts", job.getId(), job.getJobType(),
                        job.getAttempts(), ex);
            } else {
                log.warn("Job [{}] ({}) failed on attempt {}, retrying at {}", job.getId(), job.getJobType(),
                        job.getAttempts(), job.getScheduledFor(), ex);
            }
        }
        backgroundJobRepository.save(job);
    }

    /** §6.2's "02:00 daily → Cleanup DONE jobs older than 7 days" — runs directly rather than
     *  through the job queue itself (a meta-task cleaning up the queue table). */
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupOldDoneJobs() {
        long deleted = backgroundJobRepository.deleteByStatusAndCompletedAtBefore(
                JobStatus.DONE, Instant.now().minus(CLEANUP_AFTER_DAYS, ChronoUnit.DAYS));
        if (deleted > 0) {
            log.info("Cleaned up {} DONE background job(s) older than {} days", deleted, CLEANUP_AFTER_DAYS);
        }
    }
}
