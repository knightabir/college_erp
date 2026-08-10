package com.college.erp.scheduler;

import java.time.Instant;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

/** What ScheduledTasks (and any future caller) uses to put a job on the queue. */
@Component
@RequiredArgsConstructor
public class JobEnqueuer {

    private final BackgroundJobRepository backgroundJobRepository;

    public BackgroundJob enqueue(String jobType, String payloadJson) {
        return enqueue(jobType, payloadJson, Instant.now());
    }

    @Transactional
    public BackgroundJob enqueue(String jobType, String payloadJson, Instant scheduledFor) {
        BackgroundJob job = BackgroundJob.builder()
                .jobType(jobType)
                .payload(payloadJson != null ? payloadJson : "{}")
                .scheduledFor(scheduledFor)
                .build();
        return backgroundJobRepository.save(job);
    }
}
