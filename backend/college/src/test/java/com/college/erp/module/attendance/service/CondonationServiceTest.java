package com.college.erp.module.attendance.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.BusinessRuleViolationException;
import com.college.erp.common.exception.ValidationException;
import com.college.erp.module.academic.domain.Batch;
import com.college.erp.module.academic.domain.Course;
import com.college.erp.module.academic.domain.CourseLevel;
import com.college.erp.module.academic.domain.SubjectOffering;
import com.college.erp.module.academic.domain.Term;
import com.college.erp.module.academic.service.SubjectService;
import com.college.erp.module.academic.service.TermService;
import com.college.erp.module.attendance.domain.AttendanceRecordRepository;
import com.college.erp.module.attendance.domain.CondonationRequest;
import com.college.erp.module.attendance.domain.CondonationRequestRepository;
import com.college.erp.module.attendance.domain.CondonationStatus;
import com.college.erp.module.attendance.dto.CreateCondonationRequest;
import com.college.erp.module.attendance.dto.ReviewCondonationRequest;
import com.college.erp.module.student.domain.Student;
import com.college.erp.module.student.service.StudentService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CondonationServiceTest {

    @Mock
    private CondonationRequestRepository condonationRequestRepository;
    @Mock
    private AttendanceRecordRepository attendanceRecordRepository;
    @Mock
    private SubjectService subjectService;
    @Mock
    private TermService termService;
    @Mock
    private StudentService studentService;
    @Mock
    private AttendanceService attendanceService;
    @Mock
    private AttendanceRulesEngine attendanceRulesEngine;
    @Mock
    private AuditLogWriter auditLogWriter;

    private CondonationService condonationService;

    private SubjectOffering buildOffering() {
        Course course = Course.builder().name("BTech CSE").code("BTCSE").level(CourseLevel.UG)
                .durationYears(4).intakeCapacity(60).build();
        Batch batch = Batch.builder().course(course).label("CSE 2024").intakeYear(2024).build();
        SubjectOffering offering = SubjectOffering.builder().batch(batch).build();
        offering.setId(UUID.randomUUID());
        return offering;
    }

    private Student buildStudent(UUID userId) {
        Student student = Student.builder().enrollmentNumber("2024CSE001").firstName("Arjun")
                .lastName("Kumar").userId(userId).build();
        student.setId(UUID.randomUUID());
        return student;
    }

    private CondonationRequest buildRequest(CondonationStatus status) {
        Student student = buildStudent(UUID.randomUUID());
        SubjectOffering offering = buildOffering();
        Term term = Term.builder().name("Semester I").termNumber(1).build();
        term.setId(UUID.randomUUID());
        return CondonationRequest.builder()
                .student(student).offering(offering).term(term)
                .reason("Medical").status(status).build();
    }

    @Test
    @DisplayName("apply() — attendance already at/above the policy threshold is rejected")
    void apply_aboveThreshold_throws() {
        // ARRANGE
        condonationService = new CondonationService(condonationRequestRepository, attendanceRecordRepository,
                subjectService, termService, studentService, attendanceService, attendanceRulesEngine, auditLogWriter);

        UUID userId = UUID.randomUUID();
        Student student = buildStudent(userId);
        SubjectOffering offering = buildOffering();
        Term term = Term.builder().name("Semester I").termNumber(1).build();
        term.setId(UUID.randomUUID());

        when(studentService.getStudentEntityByUserId(userId)).thenReturn(student);
        when(subjectService.getOfferingEntity(offering.getId())).thenReturn(offering);
        when(termService.getTermEntity(term.getId())).thenReturn(term);
        when(attendanceRecordRepository.findByStudentAndOffering(student.getId(), offering.getId()))
                .thenReturn(List.of());
        when(attendanceService.resolveMinAttendancePct(offering)).thenReturn(75);
        when(attendanceRulesEngine.summarize(any(), any(), any(), org.mockito.ArgumentMatchers.eq(75)))
                .thenReturn(com.college.erp.module.attendance.dto.AttendanceSummaryDto.builder()
                        .attendancePercentage(90.0).eligibleForExam(true).build());

        CreateCondonationRequest request = new CreateCondonationRequest();
        request.setOfferingId(offering.getId());
        request.setTermId(term.getId());
        request.setReason("Medical emergency");

        // ACT / ASSERT
        assertThatThrownBy(() -> condonationService.apply(userId, request))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("review() — decision of PENDING is invalid")
    void review_pendingDecision_throwsValidationException() {
        // ARRANGE
        condonationService = new CondonationService(condonationRequestRepository, attendanceRecordRepository,
                subjectService, termService, studentService, attendanceService, attendanceRulesEngine, auditLogWriter);

        ReviewCondonationRequest request = new ReviewCondonationRequest();
        request.setDecision(CondonationStatus.PENDING);

        // ACT / ASSERT
        assertThatThrownBy(() -> condonationService.review(UUID.randomUUID(), request, UUID.randomUUID()))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("review() — a request already reviewed cannot be reviewed again")
    void review_alreadyReviewed_throws() {
        // ARRANGE
        condonationService = new CondonationService(condonationRequestRepository, attendanceRecordRepository,
                subjectService, termService, studentService, attendanceService, attendanceRulesEngine, auditLogWriter);

        UUID requestId = UUID.randomUUID();
        CondonationRequest existing = buildRequest(CondonationStatus.APPROVED);
        existing.setId(requestId);
        when(condonationRequestRepository.findByIdAndDeletedFalse(requestId)).thenReturn(Optional.of(existing));

        ReviewCondonationRequest request = new ReviewCondonationRequest();
        request.setDecision(CondonationStatus.REJECTED);

        // ACT / ASSERT
        assertThatThrownBy(() -> condonationService.review(requestId, request, UUID.randomUUID()))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("review() — approving a pending request records reviewer and status")
    void review_pendingRequest_approves() {
        // ARRANGE
        condonationService = new CondonationService(condonationRequestRepository, attendanceRecordRepository,
                subjectService, termService, studentService, attendanceService, attendanceRulesEngine, auditLogWriter);

        UUID requestId = UUID.randomUUID();
        UUID reviewerId = UUID.randomUUID();
        CondonationRequest existing = buildRequest(CondonationStatus.PENDING);
        existing.setId(requestId);
        when(condonationRequestRepository.findByIdAndDeletedFalse(requestId)).thenReturn(Optional.of(existing));
        when(condonationRequestRepository.save(any(CondonationRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewCondonationRequest request = new ReviewCondonationRequest();
        request.setDecision(CondonationStatus.APPROVED);
        request.setRemarks("Medical certificate verified");

        // ACT
        var result = condonationService.review(requestId, request, reviewerId);

        // ASSERT
        assertThat(result.getStatus()).isEqualTo(CondonationStatus.APPROVED);
        assertThat(result.getReviewedByUserId()).isEqualTo(reviewerId);
    }
}
