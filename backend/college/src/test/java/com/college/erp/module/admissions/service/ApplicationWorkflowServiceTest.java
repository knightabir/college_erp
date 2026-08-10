package com.college.erp.module.admissions.service;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.BusinessRuleViolationException;
import com.college.erp.module.academic.domain.AcademicYear;
import com.college.erp.module.academic.domain.Course;
import com.college.erp.module.academic.service.AcademicYearService;
import com.college.erp.module.academic.service.CourseService;
import com.college.erp.module.admissions.domain.Application;
import com.college.erp.module.admissions.domain.ApplicationDocumentRepository;
import com.college.erp.module.admissions.domain.ApplicationRepository;
import com.college.erp.module.admissions.domain.ApplicationStatus;
import com.college.erp.module.admissions.domain.ApplicationWorkflowStep;
import com.college.erp.module.admissions.domain.ApplicationWorkflowStepRepository;
import com.college.erp.module.admissions.domain.LateralEntryApplicationRepository;
import com.college.erp.module.admissions.dto.ApplicationDto;
import com.college.erp.module.admissions.dto.CreateApplicationRequest;
import com.college.erp.module.admissions.dto.ReviewApplicationRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** INVENTED module — see V015__create_admissions_tables.sql's header comment. */
@ExtendWith(MockitoExtension.class)
class ApplicationWorkflowServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private ApplicationDocumentRepository applicationDocumentRepository;
    @Mock
    private ApplicationWorkflowStepRepository applicationWorkflowStepRepository;
    @Mock
    private LateralEntryApplicationRepository lateralEntryApplicationRepository;
    @Mock
    private CourseService courseService;
    @Mock
    private AcademicYearService academicYearService;
    @Mock
    private AuditLogWriter auditLogWriter;

    private ApplicationWorkflowService applicationWorkflowService;

    private Course buildCourse() {
        Course course = Course.builder().name("B.Tech CSE").code("BTCSE").build();
        course.setId(UUID.randomUUID());
        return course;
    }

    private AcademicYear buildAcademicYear() {
        AcademicYear year = AcademicYear.builder().label("2026-2027").build();
        year.setId(UUID.randomUUID());
        return year;
    }

    private CreateApplicationRequest buildRequest(UUID courseId, UUID yearId) {
        CreateApplicationRequest request = new CreateApplicationRequest();
        request.setApplicantName("Priya Sharma");
        request.setApplicantEmail("priya@example.com");
        request.setCourseId(courseId);
        request.setAcademicYearId(yearId);
        return request;
    }

    @Test
    @DisplayName("apply() — creates a SUBMITTED application with a generated application number")
    void apply_createsSubmittedApplication() {
        // ARRANGE
        applicationWorkflowService = new ApplicationWorkflowService(applicationRepository,
                applicationDocumentRepository, applicationWorkflowStepRepository,
                lateralEntryApplicationRepository, courseService, academicYearService, auditLogWriter);
        Course course = buildCourse();
        AcademicYear year = buildAcademicYear();
        when(courseService.getCourseEntity(course.getId())).thenReturn(course);
        when(academicYearService.getAcademicYearEntity(year.getId())).thenReturn(year);
        when(applicationRepository.countByDeletedFalse()).thenReturn(0L);
        when(applicationRepository.existsByApplicationNumber(any())).thenReturn(false);
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> {
            Application application = inv.getArgument(0);
            application.setId(UUID.randomUUID());
            return application;
        });

        // ACT
        ApplicationDto dto = applicationWorkflowService.apply(buildRequest(course.getId(), year.getId()));

        // ASSERT
        assertThat(dto.getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
        assertThat(dto.getApplicationNumber()).isNotBlank();
        assertThat(dto.getApplicantName()).isEqualTo("Priya Sharma");
    }

    @Test
    @DisplayName("review() — transitions status and records a workflow step")
    void review_transitionsAndRecordsStep() {
        // ARRANGE
        applicationWorkflowService = new ApplicationWorkflowService(applicationRepository,
                applicationDocumentRepository, applicationWorkflowStepRepository,
                lateralEntryApplicationRepository, courseService, academicYearService, auditLogWriter);
        Application application = Application.builder().applicationNumber("26-00001")
                .applicantName("Priya Sharma").applicantEmail("priya@example.com")
                .course(buildCourse()).academicYear(buildAcademicYear())
                .status(ApplicationStatus.SUBMITTED).build();
        UUID applicationId = UUID.randomUUID();
        application.setId(applicationId);
        UUID reviewerId = UUID.randomUUID();
        when(applicationRepository.findByIdAndDeletedFalse(applicationId)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));
        when(applicationWorkflowStepRepository.save(any(ApplicationWorkflowStep.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ReviewApplicationRequest request = new ReviewApplicationRequest();
        request.setStatus(ApplicationStatus.SHORTLISTED);
        request.setRemarks("Strong qualifying score");

        // ACT
        ApplicationDto dto = applicationWorkflowService.review(applicationId, request, reviewerId);

        // ASSERT
        assertThat(dto.getStatus()).isEqualTo(ApplicationStatus.SHORTLISTED);
    }

    @Test
    @DisplayName("review() — an ADMITTED application cannot be reviewed further")
    void review_admittedApplication_throws() {
        // ARRANGE
        applicationWorkflowService = new ApplicationWorkflowService(applicationRepository,
                applicationDocumentRepository, applicationWorkflowStepRepository,
                lateralEntryApplicationRepository, courseService, academicYearService, auditLogWriter);
        Application application = Application.builder().applicationNumber("26-00001")
                .applicantName("Priya Sharma").applicantEmail("priya@example.com")
                .course(buildCourse()).academicYear(buildAcademicYear())
                .status(ApplicationStatus.ADMITTED).build();
        UUID applicationId = UUID.randomUUID();
        application.setId(applicationId);
        when(applicationRepository.findByIdAndDeletedFalse(applicationId)).thenReturn(Optional.of(application));

        ReviewApplicationRequest request = new ReviewApplicationRequest();
        request.setStatus(ApplicationStatus.REJECTED);

        // ACT / ASSERT
        assertThatThrownBy(() -> applicationWorkflowService.review(applicationId, request, UUID.randomUUID()))
                .isInstanceOf(BusinessRuleViolationException.class);
    }
}
