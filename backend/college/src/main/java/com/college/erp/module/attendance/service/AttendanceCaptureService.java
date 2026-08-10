package com.college.erp.module.attendance.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.BusinessRuleViolationException;
import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.module.attendance.domain.AttendanceRecord;
import com.college.erp.module.attendance.domain.AttendanceRecordRepository;
import com.college.erp.module.attendance.domain.AttendanceSession;
import com.college.erp.module.attendance.domain.AttendanceSessionRepository;
import com.college.erp.module.attendance.domain.AttendanceStatus;
import com.college.erp.module.attendance.domain.SessionStatus;
import com.college.erp.module.attendance.dto.AttendanceMarkDto;
import com.college.erp.module.student.domain.Student;
import com.college.erp.module.student.dto.GuardianDto;
import com.college.erp.module.student.service.GuardianService;
import com.college.erp.module.student.service.StudentService;
import com.college.erp.notification.NotificationRequest;
import com.college.erp.notification.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 03-srs.md [ATT-002]: re-marking is permitted while DRAFT — delete existing records for the
 * session and write fresh ones (idempotent), matching the "calling it multiple times replaces
 * the records" note on POST .../mark in 05-api-documentation.md §5.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceCaptureService {

    private static final String MODULE = "ATTENDANCE";

    private final AttendanceSessionRepository attendanceSessionRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final StudentService studentService;
    private final GuardianService guardianService;
    private final AttendanceRulesEngine attendanceRulesEngine;
    private final AuditLogWriter auditLogWriter;
    private final NotificationService notificationService;

    @Transactional
    public List<AttendanceMarkDto> mark(UUID sessionId, Map<UUID, AttendanceStatus> statusByStudentId) {
        AttendanceSession session = attendanceSessionRepository.findByIdAndDeletedFalse(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("AttendanceSession", sessionId));

        if (session.getStatus() != SessionStatus.DRAFT) {
            throw new BusinessRuleViolationException(
                    "Attendance can only be (re-)marked while the session is in DRAFT");
        }
        if (statusByStudentId.isEmpty()) {
            throw new BusinessRuleViolationException("At least one student status is required");
        }

        List<Student> students = studentService.getStudentEntities(statusByStudentId.keySet());

        attendanceRecordRepository.deleteBySessionId(sessionId);

        List<AttendanceRecord> records = students.stream()
                .map(student -> AttendanceRecord.builder()
                        .session(session)
                        .student(student)
                        .status(statusByStudentId.get(student.getId()))
                        .build())
                .toList();
        List<AttendanceRecord> saved = attendanceRecordRepository.saveAll(records);

        auditLogWriter.write(AuditAction.UPDATE, MODULE, "AttendanceSession", sessionId.toString(),
                "Marked attendance for " + saved.size() + " students");

        checkConsecutiveAbsenceAlerts(session, saved);

        return saved.stream().map(AttendanceMarkDto::from).toList();
    }

    /**
     * 03-srs.md [ATT-007]: alert on N consecutive absences. Notifies each guardian with a linked
     * user account (Guardian.userId); falls back to the student's own account if none of their
     * guardians have one — same resolution AttendanceShortfallScanJob uses for its nightly sweep.
     */
    private void checkConsecutiveAbsenceAlerts(AttendanceSession session, List<AttendanceRecord> justMarked) {
        int threshold = 3; // V005 column default; per-course AttendancePolicy override deferred —
                            // see the original version of this javadoc in git history for context.
        for (AttendanceRecord record : justMarked) {
            if (record.getStatus() != AttendanceStatus.ABSENT) {
                continue;
            }
            List<AttendanceRecord> history = attendanceRecordRepository
                    .findByStudentAndOffering(record.getStudent().getId(), session.getOffering().getId());
            List<AttendanceRecord> newestFirst = history.stream()
                    .sorted((a, b) -> b.getSession().getSessionDate().compareTo(a.getSession().getSessionDate()))
                    .toList();

            if (attendanceRulesEngine.exceedsConsecutiveAbsenceThreshold(newestFirst, threshold)) {
                notifyConsecutiveAbsence(record.getStudent(), session, threshold);
            }
        }
    }

    private void notifyConsecutiveAbsence(Student student, AttendanceSession session, int threshold) {
        String title = "Consecutive Absence Alert: " + student.getFullName();
        String body = student.getFullName() + " (" + student.getEnrollmentNumber() + ") has been absent "
                + threshold + " sessions in a row for " + session.getOffering().getSubject().getName() + ".";

        List<UUID> guardianUserIds = guardianService.listByStudent(student.getId()).stream()
                .map(GuardianDto::getUserId)
                .filter(java.util.Objects::nonNull)
                .toList();

        if (guardianUserIds.isEmpty()) {
            if (student.getUserId() == null) {
                log.warn("Student [{}] has no guardian with a linked account and no user account of "
                        + "their own — cannot notify of consecutive-absence alert",
                        student.getEnrollmentNumber());
                return;
            }
            notificationService.send(NotificationRequest.builder()
                    .userId(student.getUserId()).title(title).body(body)
                    .referenceType("Student").referenceId(student.getId().toString())
                    .build());
            return;
        }
        for (UUID guardianUserId : guardianUserIds) {
            notificationService.send(NotificationRequest.builder()
                    .userId(guardianUserId).title(title).body(body)
                    .referenceType("Student").referenceId(student.getId().toString())
                    .build());
        }
    }
}
