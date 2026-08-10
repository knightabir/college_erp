package com.college.erp.module.attendance.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.AccessDeniedException;
import com.college.erp.common.exception.BusinessRuleViolationException;
import com.college.erp.common.exception.DuplicateResourceException;
import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.module.academic.domain.Section;
import com.college.erp.module.academic.domain.SubjectOffering;
import com.college.erp.module.academic.service.SectionService;
import com.college.erp.module.academic.service.SubjectService;
import com.college.erp.module.attendance.domain.AttendancePolicy;
import com.college.erp.module.attendance.domain.AttendancePolicyRepository;
import com.college.erp.module.attendance.domain.AttendanceRecord;
import com.college.erp.module.attendance.domain.AttendanceRecordRepository;
import com.college.erp.module.attendance.domain.AttendanceSession;
import com.college.erp.module.attendance.domain.AttendanceSessionRepository;
import com.college.erp.module.attendance.domain.SessionStatus;
import com.college.erp.module.attendance.dto.AttendanceSessionDto;
import com.college.erp.module.attendance.dto.AttendanceSummaryDto;
import com.college.erp.module.attendance.dto.CreateSessionRequest;
import com.college.erp.module.attendance.dto.DefaulterDto;
import com.college.erp.module.student.domain.Student;
import com.college.erp.module.student.service.StudentService;

import lombok.RequiredArgsConstructor;

/** 03-srs.md §3.5, 05-api-documentation.md §5. */
@Service
@RequiredArgsConstructor
public class AttendanceService {

    private static final String MODULE = "ATTENDANCE";

    private final AttendanceSessionRepository attendanceSessionRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AttendancePolicyRepository attendancePolicyRepository;
    private final SubjectService subjectService;
    private final SectionService sectionService;
    private final StudentService studentService;
    private final AttendanceRulesEngine attendanceRulesEngine;
    private final AuditLogWriter auditLogWriter;

    @Transactional
    public AttendanceSessionDto openSession(CreateSessionRequest request, UUID facultyUserId) {
        if (attendanceSessionRepository.existsByOfferingIdAndSectionIdAndSessionDateAndDeletedFalse(
                request.getOfferingId(), request.getSectionId(), request.getSessionDate())) {
            throw new DuplicateResourceException(
                    "A session already exists for this offering/section/date");
        }

        SubjectOffering offering = subjectService.getOfferingEntity(request.getOfferingId());
        Section section = sectionService.getSectionEntity(request.getSectionId());

        AttendanceSession session = AttendanceSession.builder()
                .offering(offering)
                .section(section)
                .facultyUserId(facultyUserId)
                .sessionDate(request.getSessionDate())
                .status(SessionStatus.DRAFT)
                .build();

        AttendanceSession saved = attendanceSessionRepository.save(session);
        auditLogWriter.write(AuditAction.CREATE, MODULE, "AttendanceSession", saved.getId().toString(),
                "Opened session for " + offering.getSubject().getName() + " / " + section.getName());

        return AttendanceSessionDto.from(saved);
    }

    /** 03-srs.md [ATT-003]: only the assigned faculty may submit — everyone else editing after
     *  submission needs HOD approval, which is a separate, not-yet-built workflow. */
    @Transactional
    public void submitSession(UUID sessionId, UUID requestingFacultyUserId) {
        AttendanceSession session = getEntity(sessionId);

        if (!session.getFacultyUserId().equals(requestingFacultyUserId)) {
            throw new AccessDeniedException("Only the session's assigned faculty may submit it");
        }
        if (session.getStatus() != SessionStatus.DRAFT) {
            throw new BusinessRuleViolationException("Session is already submitted");
        }
        if (attendanceRecordRepository.findBySessionIdAndDeletedFalse(sessionId).isEmpty()) {
            throw new BusinessRuleViolationException("Session has no attendance marked yet");
        }

        session.setStatus(SessionStatus.SUBMITTED);
        attendanceSessionRepository.save(session);

        auditLogWriter.write(AuditAction.UPDATE, MODULE, "AttendanceSession", sessionId.toString(),
                "Submitted session");
    }

    @Transactional(readOnly = true)
    public AttendanceSummaryDto getSummary(UUID studentId, UUID offeringId) {
        SubjectOffering offering = subjectService.getOfferingEntity(offeringId);
        List<AttendanceRecord> records = attendanceRecordRepository.findByStudentAndOffering(studentId, offeringId);
        int minPct = resolveMinAttendancePct(offering);

        return attendanceRulesEngine.summarize(studentId, offeringId, records, minPct);
    }

    @Transactional(readOnly = true)
    public List<DefaulterDto> getDefaulters(UUID offeringId) {
        SubjectOffering offering = subjectService.getOfferingEntity(offeringId);
        int minPct = resolveMinAttendancePct(offering);

        return attendanceRecordRepository.findDistinctStudentIdsByOffering(offeringId).stream()
                .map(studentId -> {
                    Student student = studentService.getStudentEntity(studentId);
                    List<AttendanceRecord> records =
                            attendanceRecordRepository.findByStudentAndOffering(studentId, offeringId);
                    AttendanceSummaryDto summary =
                            attendanceRulesEngine.summarize(studentId, offeringId, records, minPct);
                    return DefaulterDto.builder()
                            .studentId(studentId)
                            .enrollmentNumber(student.getEnrollmentNumber())
                            .studentName(student.getFullName())
                            .attendancePercentage(summary.getAttendancePercentage())
                            .minRequiredPct(minPct)
                            .build();
                })
                .filter(d -> d.getAttendancePercentage() < minPct)
                .toList();
    }

    int resolveMinAttendancePct(SubjectOffering offering) {
        UUID courseId = offering.getBatch().getCourse().getId();
        return attendancePolicyRepository.findByCourseIdAndDeletedFalse(courseId)
                .map(AttendancePolicy::getMinAttendancePct)
                .orElse(AttendanceRulesEngine.DEFAULT_MIN_ATTENDANCE_PCT);
    }

    private AttendanceSession getEntity(UUID id) {
        return attendanceSessionRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("AttendanceSession", id));
    }
}
