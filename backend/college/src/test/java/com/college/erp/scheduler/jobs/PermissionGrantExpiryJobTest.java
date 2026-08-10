package com.college.erp.scheduler.jobs;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.college.erp.module.auth.domain.PermissionGrant;
import com.college.erp.module.auth.service.PermissionGrantSourceImpl;
import com.college.erp.notification.NotificationRequest;
import com.college.erp.notification.NotificationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionGrantExpiryJobTest {

    @Mock
    private PermissionGrantSourceImpl permissionGrantSource;
    @Mock
    private NotificationService notificationService;

    private PermissionGrantExpiryJob job;

    @Test
    @DisplayName("jobType() matches the JobTypes constant")
    void jobType_matchesConstant() {
        job = new PermissionGrantExpiryJob(permissionGrantSource, notificationService);
        assertThat(job.jobType()).isEqualTo(JobTypes.PERMISSION_GRANT_EXPIRY);
    }

    @Test
    @DisplayName("handle() — notifies the grantee of every grant expiring within the reminder window")
    void handle_notifiesExpiringGranteess() {
        // ARRANGE
        job = new PermissionGrantExpiryJob(permissionGrantSource, notificationService);
        UUID granteeUserId = UUID.randomUUID();
        PermissionGrant grant = PermissionGrant.builder()
                .granteeUserId(granteeUserId).grantedByUserId(UUID.randomUUID())
                .permissionKey("LIBRARY.FINE_WAIVER")
                .effectiveUntil(Instant.now().plus(2, ChronoUnit.DAYS))
                .build();
        grant.setId(UUID.randomUUID());
        when(permissionGrantSource.findExpiringBetween(any(), any())).thenReturn(List.of(grant));

        // ACT
        job.handle("{}");

        // ASSERT
        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService).send(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(granteeUserId);
        assertThat(captor.getValue().getBody()).contains("LIBRARY.FINE_WAIVER");
    }
}
