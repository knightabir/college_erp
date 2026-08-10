package com.college.erp.scheduler.jobs;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.college.erp.module.academic.domain.SubjectOffering;
import com.college.erp.module.academic.service.SubjectService;
import com.college.erp.module.attendance.dto.DefaulterDto;
import com.college.erp.module.attendance.service.AttendanceService;
import com.college.erp.module.student.domain.Student;
import com.college.erp.module.student.dto.GuardianDto;
import com.college.erp.module.student.domain.GuardianRelationship;
import com.college.erp.module.student.service.GuardianService;
import com.college.erp.module.student.service.StudentService;
import com.college.erp.notification.NotificationRequest;
import com.college.erp.notification.NotificationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceShortfallScanJobTest {

    @Mock
    private SubjectService subjectService;
    @Mock
    private AttendanceService attendanceService;
    @Mock
    private GuardianService guardianService;
    @Mock
    private StudentService studentService;
    @Mock
    private NotificationService notificationService;

    private AttendanceShortfallScanJob job;

    private SubjectOffering buildOffering() {
        SubjectOffering offering = SubjectOffering.builder().build();
        offering.setId(UUID.randomUUID());
        return offering;
    }

    private DefaulterDto buildDefaulter(UUID studentId) {
        return DefaulterDto.builder().studentId(studentId).enrollmentNumber("2024CSE001")
                .studentName("Arjun Kumar").attendancePercentage(62.5).minRequiredPct(75).build();
    }

    @Test
    @DisplayName("jobType() matches the JobTypes constant")
    void jobType_matchesConstant() {
        job = new AttendanceShortfallScanJob(subjectService, attendanceService, guardianService,
                studentService, notificationService);
        assertThat(job.jobType()).isEqualTo(JobTypes.ATTENDANCE_SHORTFALL_SCAN);
    }

    @Test
    @DisplayName("handle() — a defaulter whose guardian has a linked account: notifies the guardian, not the student")
    void handle_guardianWithLinkedAccount_notifiesGuardian() {
        // ARRANGE
        job = new AttendanceShortfallScanJob(subjectService, attendanceService, guardianService,
                studentService, notificationService);
        SubjectOffering offering = buildOffering();
        UUID studentId = UUID.randomUUID();
        UUID guardianUserId = UUID.randomUUID();
        when(subjectService.listAllOfferingEntities()).thenReturn(List.of(offering));
        when(attendanceService.getDefaulters(offering.getId())).thenReturn(List.of(buildDefaulter(studentId)));
        GuardianDto guardian = GuardianDto.builder().id(UUID.randomUUID()).studentId(studentId)
                .userId(guardianUserId).name("Ramesh Kumar").relationship(GuardianRelationship.FATHER).build();
        when(guardianService.listByStudent(studentId)).thenReturn(List.of(guardian));

        // ACT
        job.handle("{}");

        // ASSERT
        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService, times(1)).send(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(guardianUserId);
        verify(studentService, never()).getStudentEntity(any());
    }

    @Test
    @DisplayName("handle() — no guardian has a linked account: falls back to the student's own account")
    void handle_noGuardianAccount_fallsBackToStudent() {
        // ARRANGE
        job = new AttendanceShortfallScanJob(subjectService, attendanceService, guardianService,
                studentService, notificationService);
        SubjectOffering offering = buildOffering();
        UUID studentId = UUID.randomUUID();
        UUID studentUserId = UUID.randomUUID();
        when(subjectService.listAllOfferingEntities()).thenReturn(List.of(offering));
        when(attendanceService.getDefaulters(offering.getId())).thenReturn(List.of(buildDefaulter(studentId)));
        when(guardianService.listByStudent(studentId)).thenReturn(List.of());
        Student student = Student.builder().enrollmentNumber("2024CSE001").firstName("Arjun")
                .lastName("Kumar").userId(studentUserId).build();
        student.setId(studentId);
        when(studentService.getStudentEntity(studentId)).thenReturn(student);

        // ACT
        job.handle("{}");

        // ASSERT
        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService, times(1)).send(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(studentUserId);
    }

    @Test
    @DisplayName("handle() — no offerings have any defaulters: nothing sent")
    void handle_noDefaulters_sendsNothing() {
        // ARRANGE
        job = new AttendanceShortfallScanJob(subjectService, attendanceService, guardianService,
                studentService, notificationService);
        SubjectOffering offering = buildOffering();
        when(subjectService.listAllOfferingEntities()).thenReturn(List.of(offering));
        when(attendanceService.getDefaulters(offering.getId())).thenReturn(List.of());

        // ACT
        job.handle("{}");

        // ASSERT
        verify(notificationService, never()).send(any());
    }
}
