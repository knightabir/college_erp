package com.college.erp.module.attendance.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.BusinessRuleViolationException;
import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.module.academic.domain.SubjectOffering;
import com.college.erp.module.attendance.domain.AttendanceRecord;
import com.college.erp.module.attendance.domain.AttendanceRecordRepository;
import com.college.erp.module.attendance.domain.AttendanceSession;
import com.college.erp.module.attendance.domain.AttendanceSessionRepository;
import com.college.erp.module.attendance.domain.AttendanceStatus;
import com.college.erp.module.attendance.domain.SessionStatus;
import com.college.erp.module.attendance.dto.AttendanceMarkDto;
import com.college.erp.module.student.domain.Student;
import com.college.erp.module.student.service.GuardianService;
import com.college.erp.module.student.service.StudentService;
import com.college.erp.notification.NotificationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceCaptureServiceTest {

    @Mock
    private AttendanceSessionRepository attendanceSessionRepository;
    @Mock
    private AttendanceRecordRepository attendanceRecordRepository;
    @Mock
    private StudentService studentService;
    @Mock
    private GuardianService guardianService;
    @Mock
    private AttendanceRulesEngine attendanceRulesEngine;
    @Mock
    private AuditLogWriter auditLogWriter;
    @Mock
    private NotificationService notificationService;

    private AttendanceCaptureService attendanceCaptureService;

    private AttendanceSession buildSession(UUID id, SessionStatus status) {
        SubjectOffering offering = SubjectOffering.builder().build();
        offering.setId(UUID.randomUUID());
        AttendanceSession session = AttendanceSession.builder()
                .offering(offering).sessionDate(LocalDate.now()).status(status).build();
        session.setId(id);
        return session;
    }

    private Student buildStudent() {
        Student student = Student.builder().enrollmentNumber("2024CSE001").firstName("Arjun")
                .lastName("Kumar").build();
        student.setId(UUID.randomUUID());
        return student;
    }

    @Test
    @DisplayName("mark() — DRAFT session: deletes existing records and writes fresh ones")
    void mark_draftSession_replacesRecords() {
        // ARRANGE
        attendanceCaptureService = new AttendanceCaptureService(attendanceSessionRepository,
                attendanceRecordRepository, studentService, guardianService, attendanceRulesEngine,
                auditLogWriter, notificationService);
        UUID sessionId = UUID.randomUUID();
        AttendanceSession session = buildSession(sessionId, SessionStatus.DRAFT);
        Student student = buildStudent();

        when(attendanceSessionRepository.findByIdAndDeletedFalse(sessionId)).thenReturn(Optional.of(session));
        when(studentService.getStudentEntities(anyCollection())).thenReturn(List.of(student));
        when(attendanceRecordRepository.saveAll(anyCollection())).thenAnswer(inv -> inv.getArgument(0));
        // No stub for findByStudentAndOffering: the marked status is PRESENT, so
        // checkConsecutiveAbsenceAlerts's per-ABSENT loop never calls it.

        // ACT
        List<AttendanceMarkDto> result = attendanceCaptureService.mark(
                sessionId, Map.of(student.getId(), AttendanceStatus.PRESENT));

        // ASSERT
        verify(attendanceRecordRepository).deleteBySessionId(sessionId);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(AttendanceStatus.PRESENT);
    }

    @Test
    @DisplayName("mark() — SUBMITTED session rejects re-marking")
    void mark_submittedSession_throws() {
        // ARRANGE
        attendanceCaptureService = new AttendanceCaptureService(attendanceSessionRepository,
                attendanceRecordRepository, studentService, guardianService, attendanceRulesEngine,
                auditLogWriter, notificationService);
        UUID sessionId = UUID.randomUUID();
        AttendanceSession session = buildSession(sessionId, SessionStatus.SUBMITTED);
        when(attendanceSessionRepository.findByIdAndDeletedFalse(sessionId)).thenReturn(Optional.of(session));

        // ACT / ASSERT
        assertThatThrownBy(() -> attendanceCaptureService.mark(sessionId, Map.of(UUID.randomUUID(), AttendanceStatus.PRESENT)))
                .isInstanceOf(BusinessRuleViolationException.class);
        verify(attendanceRecordRepository, times(0)).deleteBySessionId(any());
    }

    @Test
    @DisplayName("mark() — empty status map is rejected")
    void mark_emptyMap_throws() {
        // ARRANGE
        attendanceCaptureService = new AttendanceCaptureService(attendanceSessionRepository,
                attendanceRecordRepository, studentService, guardianService, attendanceRulesEngine,
                auditLogWriter, notificationService);
        UUID sessionId = UUID.randomUUID();
        AttendanceSession session = buildSession(sessionId, SessionStatus.DRAFT);
        when(attendanceSessionRepository.findByIdAndDeletedFalse(sessionId)).thenReturn(Optional.of(session));

        // ACT / ASSERT
        assertThatThrownBy(() -> attendanceCaptureService.mark(sessionId, Map.of()))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("mark() — unknown session throws ResourceNotFoundException")
    void mark_unknownSession_throws() {
        // ARRANGE
        attendanceCaptureService = new AttendanceCaptureService(attendanceSessionRepository,
                attendanceRecordRepository, studentService, guardianService, attendanceRulesEngine,
                auditLogWriter, notificationService);
        UUID sessionId = UUID.randomUUID();
        when(attendanceSessionRepository.findByIdAndDeletedFalse(sessionId)).thenReturn(Optional.empty());

        // ACT / ASSERT
        assertThatThrownBy(() -> attendanceCaptureService.mark(sessionId, Map.of(UUID.randomUUID(), AttendanceStatus.PRESENT)))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
