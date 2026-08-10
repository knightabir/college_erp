package com.college.erp.scheduler.jobs;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.college.erp.module.academic.domain.SubjectOffering;
import com.college.erp.module.academic.service.SubjectService;
import com.college.erp.module.attendance.dto.DefaulterDto;
import com.college.erp.module.attendance.service.AttendanceService;
import com.college.erp.module.student.dto.GuardianDto;
import com.college.erp.module.student.service.GuardianService;
import com.college.erp.module.student.service.StudentService;
import com.college.erp.notification.NotificationRequest;
import com.college.erp.notification.NotificationService;
import com.college.erp.scheduler.JobHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 03-srs.md [SCHED-004]: "Attendance shortfall scan: nightly at 01:00." System-wide sweep — for
 * every subject offering, find students below the course's minimum attendance percentage
 * (reusing AttendanceService.getDefaulters(), the same query the on-demand
 * GET /attendance/defaulters/{offeringId} endpoint uses) and notify them.
 *
 * Distinct from [ATT-007]'s real-time "N consecutive absences" alert (wired directly into
 * AttendanceCaptureService.mark(), not this job) — this is the periodic "your cumulative
 * percentage is below the course minimum" sweep.
 *
 * Notifies each defaulter's guardian(s) if a guardian has a linked user account
 * (Guardian.userId — see GuardianDto javadoc for why that's not always populated), falling back to
 * notifying the student directly otherwise. This resolves the "parent notification not yet wired
 * up" gap AttendanceCaptureService's own javadoc used to flag before this job existed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AttendanceShortfallScanJob implements JobHandler {

    private final SubjectService subjectService;
    private final AttendanceService attendanceService;
    private final GuardianService guardianService;
    private final StudentService studentService;
    private final NotificationService notificationService;

    @Override
    public String jobType() {
        return JobTypes.ATTENDANCE_SHORTFALL_SCAN;
    }

    @Override
    public void handle(String payloadJson) {
        List<SubjectOffering> offerings = subjectService.listAllOfferingEntities();
        int notified = 0;
        for (SubjectOffering offering : offerings) {
            List<DefaulterDto> defaulters = attendanceService.getDefaulters(offering.getId());
            for (DefaulterDto defaulter : defaulters) {
                notifyDefaulter(defaulter);
                notified++;
            }
        }
        log.info("AttendanceShortfallScanJob: notified {} defaulter(s) across {} offering(s)",
                notified, offerings.size());
    }

    private void notifyDefaulter(DefaulterDto defaulter) {
        String title = "Attendance shortfall: " + defaulter.getStudentName();
        String body = String.format(
                "%s (%s) is at %.1f%% attendance, below the required %d%%.",
                defaulter.getStudentName(), defaulter.getEnrollmentNumber(),
                defaulter.getAttendancePercentage(), defaulter.getMinRequiredPct());

        List<GuardianDto> guardians = guardianService.listByStudent(defaulter.getStudentId());
        List<UUID> guardianUserIds = guardians.stream()
                .map(GuardianDto::getUserId)
                .filter(java.util.Objects::nonNull)
                .toList();

        if (guardianUserIds.isEmpty()) {
            // No guardian has a linked user account — fall back to notifying the student's own
            // account (Student.userId, distinct from Student.id — see class javadoc).
            UUID studentUserId = studentService.getStudentEntity(defaulter.getStudentId()).getUserId();
            if (studentUserId == null) {
                log.warn("Defaulter [{}] has neither a guardian with a linked account nor a student "
                        + "user account — cannot notify", defaulter.getEnrollmentNumber());
                return;
            }
            notificationService.send(NotificationRequest.builder()
                    .userId(studentUserId)
                    .title(title).body(body)
                    .referenceType("STUDENT").referenceId(defaulter.getStudentId().toString())
                    .build());
            return;
        }
        for (UUID guardianUserId : guardianUserIds) {
            notificationService.send(NotificationRequest.builder()
                    .userId(guardianUserId)
                    .title(title).body(body)
                    .referenceType("STUDENT").referenceId(defaulter.getStudentId().toString())
                    .build());
        }
    }
}
