package com.college.erp.scheduler;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 03-srs.md [SCHED-002]/[SCHED-003]. Named to match the kernel's other *DispatcherTest files. */
@ExtendWith(MockitoExtension.class)
class JobDispatcherTest {

    @Mock
    private BackgroundJobRepository backgroundJobRepository;

    @Test
    @DisplayName("pollAndDispatch() — a successful handler marks the job DONE")
    void pollAndDispatch_successfulHandler_marksDone() {
        // ARRANGE
        JobHandler succeeding = new JobHandler() {
            @Override
            public String jobType() {
                return "SUCCEED";
            }

            @Override
            public void handle(String payloadJson) {
                // no-op — succeeds
            }
        };
        JobDispatcher dispatcher = new JobDispatcher(backgroundJobRepository, List.of(succeeding));
        BackgroundJob job = BackgroundJob.builder().jobType("SUCCEED").maxAttempts(3).build();
        when(backgroundJobRepository.findDuePendingForUpdateSkipLocked(10)).thenReturn(List.of(job));

        // ACT
        dispatcher.pollAndDispatch();

        // ASSERT
        assertThat(job.getStatus()).isEqualTo(JobStatus.DONE);
        verify(backgroundJobRepository).save(job);
    }

    @Test
    @DisplayName("pollAndDispatch() — a failing handler under maxAttempts reschedules as PENDING")
    void pollAndDispatch_failingHandlerUnderMax_reschedules() {
        // ARRANGE
        JobHandler failing = new JobHandler() {
            @Override
            public String jobType() {
                return "FAIL";
            }

            @Override
            public void handle(String payloadJson) throws Exception {
                throw new IllegalStateException("boom");
            }
        };
        JobDispatcher dispatcher = new JobDispatcher(backgroundJobRepository, List.of(failing));
        BackgroundJob job = BackgroundJob.builder().jobType("FAIL").maxAttempts(3).attempts(0).build();
        when(backgroundJobRepository.findDuePendingForUpdateSkipLocked(10)).thenReturn(List.of(job));

        // ACT
        dispatcher.pollAndDispatch();

        // ASSERT
        assertThat(job.getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(job.getLastError()).isEqualTo("boom");
        assertThat(job.getAttempts()).isEqualTo(1);
    }

    @Test
    @DisplayName("pollAndDispatch() — a failing handler at maxAttempts marks the job FAILED")
    void pollAndDispatch_failingHandlerAtMax_marksFailed() {
        // ARRANGE
        JobHandler failing = new JobHandler() {
            @Override
            public String jobType() {
                return "FAIL";
            }

            @Override
            public void handle(String payloadJson) throws Exception {
                throw new IllegalStateException("boom");
            }
        };
        JobDispatcher dispatcher = new JobDispatcher(backgroundJobRepository, List.of(failing));
        BackgroundJob job = BackgroundJob.builder().jobType("FAIL").maxAttempts(1).attempts(0).build();
        when(backgroundJobRepository.findDuePendingForUpdateSkipLocked(10)).thenReturn(List.of(job));

        // ACT
        dispatcher.pollAndDispatch();

        // ASSERT
        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
    }

    @Test
    @DisplayName("pollAndDispatch() — no registered handler for the job's type: retried like any other failure")
    void pollAndDispatch_noHandler_treatedAsFailure() {
        // ARRANGE
        JobDispatcher dispatcher = new JobDispatcher(backgroundJobRepository, List.of());
        BackgroundJob job = BackgroundJob.builder().jobType("UNKNOWN").maxAttempts(3).attempts(0).build();
        when(backgroundJobRepository.findDuePendingForUpdateSkipLocked(10)).thenReturn(List.of(job));

        // ACT
        dispatcher.pollAndDispatch();

        // ASSERT
        assertThat(job.getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(job.getLastError()).contains("No JobHandler");
    }

    @Test
    @DisplayName("cleanupOldDoneJobs() — deletes DONE jobs older than 7 days")
    void cleanupOldDoneJobs_deletesOldDoneJobs() {
        // ARRANGE
        JobDispatcher dispatcher = new JobDispatcher(backgroundJobRepository, List.of());
        when(backgroundJobRepository.deleteByStatusAndCompletedAtBefore(any(), any())).thenReturn(5L);

        // ACT
        dispatcher.cleanupOldDoneJobs();

        // ASSERT
        ArgumentCaptor<JobStatus> statusCaptor = ArgumentCaptor.forClass(JobStatus.class);
        verify(backgroundJobRepository, times(1)).deleteByStatusAndCompletedAtBefore(statusCaptor.capture(), any());
        assertThat(statusCaptor.getValue()).isEqualTo(JobStatus.DONE);
    }
}
