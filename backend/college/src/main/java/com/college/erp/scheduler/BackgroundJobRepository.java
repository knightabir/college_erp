package com.college.erp.scheduler;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BackgroundJobRepository extends JpaRepository<BackgroundJob, UUID> {

    /** [SCHED-002]: due PENDING jobs, oldest-scheduled first, row-locked so multiple app instances
     *  polling the same table never double-process a job — 04-database-schema.md §16.3 / [NFR-SCALE-002]. */
    @Query(value = """
        SELECT * FROM background_jobs
        WHERE status = 'PENDING' AND scheduled_for <= NOW()
        ORDER BY scheduled_for ASC
        LIMIT :limit
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<BackgroundJob> findDuePendingForUpdateSkipLocked(@Param("limit") int limit);

    /** §6.2's "02:00 daily → Cleanup DONE jobs older than 7 days". */
    long deleteByStatusAndCompletedAtBefore(JobStatus status, Instant completedBefore);
}
