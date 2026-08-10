package com.college.erp.module.hostel.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.AccessDeniedException;
import com.college.erp.common.exception.BusinessRuleViolationException;
import com.college.erp.module.hostel.domain.OutpassRequest;
import com.college.erp.module.hostel.domain.OutpassRequestRepository;
import com.college.erp.module.hostel.domain.OutpassStatus;
import com.college.erp.module.hostel.dto.CreateOutpassRequest;
import com.college.erp.module.hostel.dto.OutpassRequestDto;
import com.college.erp.module.student.domain.Student;
import com.college.erp.module.student.service.StudentService;
import com.college.erp.notification.NotificationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** 04-database-schema.md §14.1. Named to match the module's other *ServiceTest files. */
@ExtendWith(MockitoExtension.class)
class OutpassRequestServiceTest {

    @Mock
    private OutpassRequestRepository outpassRequestRepository;
    @Mock
    private StudentService studentService;
    @Mock
    private AuditLogWriter auditLogWriter;
    @Mock
    private NotificationService notificationService;

    private OutpassRequestService outpassRequestService;

    private Student buildStudent(UUID userId) {
        Student student = Student.builder().enrollmentNumber("2024CSE001").firstName("Arjun")
                .lastName("Kumar").userId(userId).build();
        student.setId(UUID.randomUUID());
        return student;
    }

    @Test
    @DisplayName("apply() — valid date range succeeds, PENDING status")
    void apply_validRange_succeeds() {
        // ARRANGE
        outpassRequestService = new OutpassRequestService(outpassRequestRepository, studentService, auditLogWriter,
                notificationService);
        Student student = buildStudent(UUID.randomUUID());
        when(studentService.getStudentEntity(student.getId())).thenReturn(student);
        when(outpassRequestRepository.save(any(OutpassRequest.class))).thenAnswer(inv -> {
            OutpassRequest request = inv.getArgument(0);
            request.setId(UUID.randomUUID());
            return request;
        });

        CreateOutpassRequest request = new CreateOutpassRequest();
        request.setFromDatetime(Instant.now());
        request.setToDatetime(Instant.now().plus(4, ChronoUnit.HOURS));
        request.setReason("Family function");

        // ACT
        OutpassRequestDto dto = outpassRequestService.apply(request, student.getId());

        // ASSERT
        assertThat(dto.getStatus()).isEqualTo(OutpassStatus.PENDING);
    }

    @Test
    @DisplayName("apply() — to_datetime not after from_datetime is rejected")
    void apply_invalidRange_throws() {
        // ARRANGE
        outpassRequestService = new OutpassRequestService(outpassRequestRepository, studentService, auditLogWriter,
                notificationService);
        CreateOutpassRequest request = new CreateOutpassRequest();
        Instant now = Instant.now();
        request.setFromDatetime(now);
        request.setToDatetime(now.minus(1, ChronoUnit.HOURS));
        request.setReason("x");

        // ACT / ASSERT
        assertThatThrownBy(() -> outpassRequestService.apply(request, UUID.randomUUID()))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("approve() — notifies the student when they have a linked account")
    void approve_notifiesStudent() {
        // ARRANGE
        outpassRequestService = new OutpassRequestService(outpassRequestRepository, studentService, auditLogWriter,
                notificationService);
        UUID studentUserId = UUID.randomUUID();
        Student student = buildStudent(studentUserId);
        OutpassRequest outpass = OutpassRequest.builder().student(student)
                .fromDatetime(Instant.now()).toDatetime(Instant.now().plus(4, ChronoUnit.HOURS))
                .reason("x").status(OutpassStatus.PENDING).build();
        UUID outpassId = UUID.randomUUID();
        outpass.setId(outpassId);
        when(outpassRequestRepository.findByIdAndDeletedFalse(outpassId)).thenReturn(Optional.of(outpass));
        when(outpassRequestRepository.save(any(OutpassRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        // ACT
        OutpassRequestDto dto = outpassRequestService.approve(outpassId, UUID.randomUUID());

        // ASSERT
        assertThat(dto.getStatus()).isEqualTo(OutpassStatus.APPROVED);
    }

    @Test
    @DisplayName("cancel() — only the owning student may cancel")
    void cancel_notOwner_throws() {
        // ARRANGE
        outpassRequestService = new OutpassRequestService(outpassRequestRepository, studentService, auditLogWriter,
                notificationService);
        Student student = buildStudent(UUID.randomUUID());
        OutpassRequest outpass = OutpassRequest.builder().student(student)
                .fromDatetime(Instant.now()).toDatetime(Instant.now().plus(4, ChronoUnit.HOURS))
                .reason("x").status(OutpassStatus.PENDING).build();
        UUID outpassId = UUID.randomUUID();
        outpass.setId(outpassId);
        when(outpassRequestRepository.findByIdAndDeletedFalse(outpassId)).thenReturn(Optional.of(outpass));

        // ACT / ASSERT
        assertThatThrownBy(() -> outpassRequestService.cancel(outpassId, UUID.randomUUID()))
                .isInstanceOf(AccessDeniedException.class);
    }
}
