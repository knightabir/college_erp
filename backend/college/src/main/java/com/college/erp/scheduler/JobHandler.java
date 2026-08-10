package com.college.erp.scheduler;

/**
 * 08-system-architecture.md's file tree: `scheduler/jobs/*.java`. Implement one bean per
 * job_type; JobDispatcher discovers all JobHandler beans and routes by jobType().
 */
public interface JobHandler {

    /** Must match the job_type string used when the job was enqueued (see JobEnqueuer). */
    String jobType();

    /** Throw to signal failure — JobDispatcher handles the retry/backoff/FAILED bookkeeping. */
    void handle(String payloadJson) throws Exception;
}
