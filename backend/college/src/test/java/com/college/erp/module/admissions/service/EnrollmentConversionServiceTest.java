package com.college.erp.module.admissions.service;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.BusinessRuleViolationException;
import com.college.erp.module.academic.domain.AcademicYear;
import com.college.erp.module.academic.domain.Batch;
import com.college.erp.module.academic.domain.Course;
import com.college.erp.module.admissions.domain.Application;
import com.college.erp.module.admissions.domain.ApplicationRepository;
import com.college.erp.module.admissions.domain.ApplicationStatus;
import com.college.erp.module.admissions.domain.SeatAllotment;
import com.college.erp.module.admissions.domain.SeatAllotmentStatus;
import com.college.erp.module.admissions.dto.ConvertToStudentRequest;
import com.college.erp.module.student.dto.StudentDto;
import com.college.erp.module.student.service.StudentService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** INVENTED module — see V015__create_admissions_tables.sql's header comment. */
@ExtendWith(MockitoExtension.class)
class EnrollmentConversionServiceTest {

    @Mock
    private ApplicationWorkflowService applicationWorkflowService;
    @Mock
    private MeritProcessingService meritProcessingService;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private StudentService studentService;
    @Mock
    private AuditLogWriter auditLogWriter;

    private EnrollmentConversionService enrollmentConversionService;

    private Application buildApplication() {
        Course course = Course.builder().name("B.Tech CSE").build();
        course.setId(UUID.randomUUID());
        AcademicYear year = AcademicYear.builder().label("2026-2027").build();
        year.setId(UUID.randomUUID());
        Application application = Application.builder().applicationNumber("26-00001")
                .applicantName("Priya Sharma").applicantEmail("priya@example.com")
                .dateOfBirth(LocalDate.of(2007, 5, 10)).gender("FEMALE")
                .course(course).academicYear(year).status(ApplicationStatus.SEAT_ALLOTTED).build();
        application.setId(UUID.randomUUID());
        return application;
    }

    @Test
    @DisplayName("convert() — requires an ACCEPTED seat allotment")
    void convert_seatNotAccepted_throws() {
        // ARRANGE
        enrollmentConversionService = new EnrollmentConversionService(applicationWorkflowService,
                meritProcessingService, applicationRepository, studentService, auditLogWriter);
        Application application = buildApplication();
        when(applicationWorkflowService.getApplicationEntity(application.getId())).thenReturn(application);
        when(meritProcessingService.getAcceptedSeatAllotmentEntity(application.getId()))
                .thenThrow(new BusinessRuleViolationException("Seat allotment must be ACCEPTED"));

        ConvertToStudentRequest request = new ConvertToStudentRequest();
        request.setEnrollmentNumber("2026CSE099");
        request.setSectionId(UUID.randomUUID());

        // ACT / ASSERT
        assertThatThrownBy(() -> enrollmentConversionService.convert(application.getId(), request, UUID.randomUUID()))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("convert() — an accepted seat allotment creates the student and admits the application")
    void convert_acceptedSeat_createsStudentAndAdmits() {
        // ARRANGE
        enrollmentConversionService = new EnrollmentConversionService(applicationWorkflowService,
                meritProcessingService, applicationRepository, studentService, auditLogWriter);
        Application application = buildApplication();
        Batch batch = Batch.builder().build();
        batch.setId(UUID.randomUUID());
        SeatAllotment allotment = SeatAllotment.builder().application(application).course(application.getCourse())
                .batch(batch).status(SeatAllotmentStatus.ACCEPTED).build();

        when(applicationWorkflowService.getApplicationEntity(application.getId())).thenReturn(application);
        when(meritProcessingService.getAcceptedSeatAllotmentEntity(application.getId())).thenReturn(allotment);
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        StudentDto expectedStudent = StudentDto.builder().id(UUID.randomUUID())
                .enrollmentNumber("2026CSE099").fullName("Priya Sharma").build();
        when(studentService.create(any())).thenReturn(expectedStudent);

        ConvertToStudentRequest request = new ConvertToStudentRequest();
        request.setEnrollmentNumber("2026CSE099");
        request.setSectionId(UUID.randomUUID());

        // ACT
        StudentDto result = enrollmentConversionService.convert(application.getId(), request, UUID.randomUUID());

        // ASSERT
        assertThat(result.getEnrollmentNumber()).isEqualTo("2026CSE099");
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.ADMITTED);
    }
}
