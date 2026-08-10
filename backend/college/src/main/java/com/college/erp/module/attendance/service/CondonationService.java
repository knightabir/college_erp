package com.college.erp.module.attendance.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.BusinessRuleViolationException;
import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.common.exception.ValidationException;
import com.college.erp.module.academic.domain.SubjectOffering;
import com.college.erp.module.academic.domain.Term;
import com.college.erp.module.academic.service.SubjectService;
import com.college.erp.module.academic.service.TermService;
import com.college.erp.module.attendance.domain.AttendanceRecord;
import com.college.erp.module.attendance.domain.AttendanceRecordRepository;
import com.college.erp.module.attendance.domain.CondonationRequest;
import com.college.erp.module.attendance.domain.CondonationRequestRepository;
import com.college.erp.module.attendance.domain.CondonationStatus;
import com.college.erp.module.attendance.dto.CondonationRequestDto;
import com.college.erp.module.attendance.dto.CreateCondonationRequest;
import com.college.erp.module.attendance.dto.ReviewCondonationRequest;
import com.college.erp.module.student.domain.Student;
import com.college.erp.module.student.service.StudentService;

import lombok.RequiredArgsConstructor;

/** 03-srs.md [ATT-006], 05-api-documentation.md §5 POST/PATCH /attendance/condonation. */
@Service
@RequiredArgsConstructor
public class CondonationService {

    private static final String MODULE = "ATTENDANCE";

    private final CondonationRequestRepository condonationRequestRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final SubjectService subjectService;
    private final TermService termService;
    private final StudentService studentService;
    private final AttendanceService attendanceService;
    private final AttendanceRulesEngine attendanceRulesEngine;
    private final AuditLogWriter auditLogWriter;

    @Transactional
    public CondonationRequestDto apply(UUID requestingUserId, CreateCondonationRequest request) {
        Student student = studentService.getStudentEntityByUserId(requestingUserId);
        UUID studentId = student.getId();
        SubjectOffering offering = subjectService.getOfferingEntity(request.getOfferingId());
        Term term = termService.getTermEntity(request.getTermId());

        // 05-api-documentation.md §5: 422 if attendance is already above minimum threshold.
        List<AttendanceRecord> records = attendanceRecordRepository.findByStudentAndOffering(studentId, offering.getId());
        int minPct = attendanceService.resolveMinAttendancePct(offering);
        double percentage = attendanceRulesEngine.summarize(studentId, offering.getId(), records, minPct)
                .getAttendancePercentage();
        if (percentage >= minPct) {
            throw new BusinessRuleViolationException(
                    "Attendance is already above the minimum threshold (" + minPct + "%) — condonation not needed");
        }

        CondonationRequest condonationRequest = CondonationRequest.builder()
                .student(student)
                .offering(offering)
                .term(term)
                .reason(request.getReason())
                .status(CondonationStatus.PENDING)
                .build();

        CondonationRequest saved = condonationRequestRepository.save(condonationRequest);
        auditLogWriter.write(AuditAction.CREATE, MODULE, "CondonationRequest", saved.getId().toString(),
                "Condonation requested by " + student.getEnrollmentNumber());

        return CondonationRequestDto.from(saved);
    }

    /** ATT-006: approval does not change the attendance percentage — it's a flag the (not yet
     *  built) exam module reads when generating hall tickets. */
    @Transactional
    public CondonationRequestDto review(UUID requestId, ReviewCondonationRequest request, UUID reviewerUserId) {
        if (request.getDecision() == CondonationStatus.PENDING) {
            throw new ValidationException("decision must be APPROVED or REJECTED");
        }

        CondonationRequest condonationRequest = condonationRequestRepository.findByIdAndDeletedFalse(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("CondonationRequest", requestId));

        if (condonationRequest.getStatus() != CondonationStatus.PENDING) {
            throw new BusinessRuleViolationException("This request has already been reviewed");
        }

        condonationRequest.setStatus(request.getDecision());
        condonationRequest.setReviewedByUserId(reviewerUserId);
        condonationRequest.setReviewRemarks(request.getRemarks());

        CondonationRequest saved = condonationRequestRepository.save(condonationRequest);
        auditLogWriter.write(AuditAction.UPDATE, MODULE, "CondonationRequest", saved.getId().toString(),
                "Reviewed: " + saved.getStatus());

        return CondonationRequestDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<CondonationRequestDto> listByStudent(UUID studentId) {
        return condonationRequestRepository.findByStudentIdAndDeletedFalse(studentId).stream()
                .map(CondonationRequestDto::from)
                .toList();
    }
}
