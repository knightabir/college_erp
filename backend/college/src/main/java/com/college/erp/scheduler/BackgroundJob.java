package com.college.erp.scheduler;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 04-database-schema.md §16.3, 03-srs.md §3.12 [SCHED-001]-[SCHED-003]. No is_deleted/updated_at —
 * a job row's lifecycle is fully captured by status/attempts/timestamps, and completed jobs are
 * physically deleted by the nightly cleanup sweep (§6.2's "02:00 daily → Cleanup DONE jobs older
 * than 7 days") rather than soft-deleted.
 */
@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "background_jobs")
public class BackgroundJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "job_type", nullable = false, updatable = false, length = 100)
    private String jobType;

    /** JSON payload — mapped straight to `jsonb` via Hibernate 6's JSON JdbcType, no extra
     *  serialization library needed. */
    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload = "{}";

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private JobStatus status = JobStatus.PENDING;

    @Builder.Default
    @Column(name = "attempts", nullable = false)
    private int attempts = 0;

    @Builder.Default
    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts = 3;

    @Builder.Default
    @Column(name = "scheduled_for", nullable = false)
    private Instant scheduledFor = Instant.now();

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** [SCHED-002]: claimed by the poller — status RUNNING, attempts incremented, started_at set. */
    public void markRunning() {
        this.status = JobStatus.RUNNING;
        this.attempts++;
        this.startedAt = Instant.now();
    }

    public void markDone() {
        this.status = JobStatus.DONE;
        this.completedAt = Instant.now();
    }

    /** [SCHED-003]: retries with exponential backoff while under maxAttempts, else FAILED. */
    public void markFailedOrRetry(String error) {
        this.lastError = error;
        if (this.attempts >= this.maxAttempts) {
            this.status = JobStatus.FAILED;
            this.completedAt = Instant.now();
        } else {
            this.status = JobStatus.PENDING;
            long backoffSeconds = (long) Math.pow(2, this.attempts) * 30;
            this.scheduledFor = Instant.now().plusSeconds(backoffSeconds);
        }
    }
}
