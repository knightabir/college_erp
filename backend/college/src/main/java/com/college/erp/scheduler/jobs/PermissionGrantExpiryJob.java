package com.college.erp.scheduler.jobs;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Component;

import com.college.erp.module.auth.domain.PermissionGrant;
import com.college.erp.module.auth.service.PermissionGrantSourceImpl;
import com.college.erp.notification.NotificationRequest;
import com.college.erp.notification.NotificationService;
import com.college.erp.scheduler.JobHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 03-srs.md [SCHED-004]: "Permission grant expiry: nightly at 03:00." Notifies every grantee whose
 * scoped permission_grants row (Layer 2 of RBAC — see GrantResolver) expires within
 * REMINDER_WINDOW_DAYS, so they know a scoped permission is about to lapse.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionGrantExpiryJob implements JobHandler {

    private static final int REMINDER_WINDOW_DAYS = 3;

    private final PermissionGrantSourceImpl permissionGrantSource;
    private final NotificationService notificationService;

    @Override
    public String jobType() {
        return JobTypes.PERMISSION_GRANT_EXPIRY;
    }

    @Override
    public void handle(String payloadJson) {
        Instant now = Instant.now();
        List<PermissionGrant> expiring = permissionGrantSource.findExpiringBetween(
                now, now.plus(REMINDER_WINDOW_DAYS, ChronoUnit.DAYS));

        for (PermissionGrant grant : expiring) {
            notificationService.send(NotificationRequest.builder()
                    .userId(grant.getGranteeUserId())
                    .title("Permission Expiring Soon")
                    .body("Your grant for '" + grant.getPermissionKey() + "' expires on "
                            + grant.getEffectiveUntil() + ".")
                    .referenceType("PermissionGrant").referenceId(grant.getId().toString())
                    .build());
        }
        log.info("PermissionGrantExpiryJob: notified {} expiring grant(s)", expiring.size());
    }
}
