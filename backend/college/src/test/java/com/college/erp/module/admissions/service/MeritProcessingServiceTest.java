package com.college.erp.module.admissions.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.BusinessRuleViolationException;
import com.college.erp.module.academic.domain.AcademicYear;
import com.college.erp.module.academic.domain.Batch;
import com.college.erp.module.academic.domain.Course;
import com.college.erp.module.academic.service.AcademicYearService;
import com.college.erp.module.academic.service.BatchService;
import com.college.erp.module.academic.service.CourseService;
import com.college.erp.module.admissions.domain.Application;
import com.college.erp.module.admissions.domain.ApplicationRepository;
import com.college.erp.module.admissions.domain.ApplicationStatus;
import com.college.erp.module.admissions.domain.MeritList;
import com.college.erp.module.admissions.domain.MeritListEntry;
import com.college.erp.module.admissions.domain.MeritListEntryRepository;
import com.college.erp.module.admissions.domain.MeritListRepository;
import com.college.erp.module.admissions.domain.SeatAllotment;
import com.college.erp.module.admissions.domain.SeatAllotmentRepository;
import com.college.erp.module.admissions.domain.SeatAllotmentStatus;
import com.college.erp.module.admissions.dto.GenerateMeritListRequest;
import com.college.erp.module.admissions.dto.MeritListDto;
import com.college.erp.module.admissions.dto.OfferSeatRequest;
import com.college.erp.module.admissions.dto.SeatAllotmentDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** INVENTED module — see V015__create_admissions_tables.sql's header comment. */
@ExtendWith(MockitoExtension.class)
class MeritProcessingServiceTest {

    @Mock
    private MeritListRepository meritListRepository;
    @Mock
    private MeritListEntryRepository meritListEntryRepository;
    @Mock
    private SeatAllotmentRepository seatAllotmentRepository;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private ApplicationWorkflowService applicationWorkflowService;
    @Mock
    private CourseService courseService;
    @Mock
    private AcademicYearService academicYearService;
    @Mock
    private BatchService batchService;
    @Mock
    private AuditLogWriter auditLogWriter;

    private MeritProcessingService meritProcessingService;

    private Course buildCourse() {
        Course course = Course.builder().name("B.Tech CSE").code("BTCSE").build();
        course.setId(UUID.randomUUID());
        return course;
    }

    private AcademicYear buildYear() {
        AcademicYear year = AcademicYear.builder().label("2026-2027").build();
        year.setId(UUID.randomUUID());
        return year;
    }

    private Application buildApplication(Course course, AcademicYear year, BigDecimal score) {
        Application application = Application.builder().applicationNumber("26-0000" + score)
                .applicantName("Applicant " + score).applicantEmail("a@example.com")
                .course(course).academicYear(year).qualifyingExamScore(score)
                .status(ApplicationStatus.SHORTLISTED).build();
        application.setId(UUID.randomUUID());
        return application;
    }

    @Test
    @DisplayName("generate() — ranks SHORTLISTED applications by score, descending, rank 1 = highest")
    void generate_ranksByScoreDescending() {
        // ARRANGE
        meritProcessingService = new MeritProcessingService(meritListRepository, meritListEntryRepository,
                seatAllotmentRepository, applicationRepository, applicationWorkflowService, courseService,
                academicYearService, batchService, auditLogWriter);
        Course course = buildCourse();
        AcademicYear year = buildYear();
        Application low = buildApplication(course, year, BigDecimal.valueOf(70));
        Application high = buildApplication(course, year, BigDecimal.valueOf(95));
        Application mid = buildApplication(course, year, BigDecimal.valueOf(82));

        when(courseService.getCourseEntity(course.getId())).thenReturn(course);
        when(academicYearService.getAcademicYearEntity(year.getId())).thenReturn(year);
        when(applicationRepository.findByCourseIdAndAcademicYearIdAndStatusAndDeletedFalse(
                course.getId(), year.getId(), ApplicationStatus.SHORTLISTED))
                .thenReturn(List.of(low, high, mid));
        when(meritListRepository.save(any(MeritList.class))).thenAnswer(inv -> {
            MeritList meritList = inv.getArgument(0);
            meritList.setId(UUID.randomUUID());
            return meritList;
        });
        ArgumentCaptor<MeritListEntry> entryCaptor = ArgumentCaptor.forClass(MeritListEntry.class);
        when(meritListEntryRepository.save(entryCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        GenerateMeritListRequest request = new GenerateMeritListRequest();
        request.setCourseId(course.getId());
        request.setAcademicYearId(year.getId());
        request.setName("CSE Round 1");

        // ACT
        MeritListDto dto = meritProcessingService.generate(request);

        // ASSERT
        assertThat(dto.getName()).isEqualTo("CSE Round 1");
        List<MeritListEntry> entries = entryCaptor.getAllValues();
        assertThat(entries).hasSize(3);
        assertThat(entries.get(0).getApplication()).isEqualTo(high);
        assertThat(entries.get(0).getRank()).isEqualTo(1);
        assertThat(entries.get(1).getApplication()).isEqualTo(mid);
        assertThat(entries.get(2).getApplication()).isEqualTo(low);
        assertThat(high.getStatus()).isEqualTo(ApplicationStatus.MERIT_LISTED);
    }

    @Test
    @DisplayName("generate() — no SHORTLISTED applications: rejected")
    void generate_noShortlisted_throws() {
        // ARRANGE
        meritProcessingService = new MeritProcessingService(meritListRepository, meritListEntryRepository,
                seatAllotmentRepository, applicationRepository, applicationWorkflowService, courseService,
                academicYearService, batchService, auditLogWriter);
        Course course = buildCourse();
        AcademicYear year = buildYear();
        when(courseService.getCourseEntity(course.getId())).thenReturn(course);
        when(academicYearService.getAcademicYearEntity(year.getId())).thenReturn(year);
        when(applicationRepository.findByCourseIdAndAcademicYearIdAndStatusAndDeletedFalse(
                course.getId(), year.getId(), ApplicationStatus.SHORTLISTED))
                .thenReturn(List.of());

        GenerateMeritListRequest request = new GenerateMeritListRequest();
        request.setCourseId(course.getId());
        request.setAcademicYearId(year.getId());
        request.setName("Empty round");

        // ACT / ASSERT
        assertThatThrownBy(() -> meritProcessingService.generate(request))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("offerSeat() — only a MERIT_LISTED application may be offered a seat")
    void offerSeat_notMeritListed_throws() {
        // ARRANGE
        meritProcessingService = new MeritProcessingService(meritListRepository, meritListEntryRepository,
                seatAllotmentRepository, applicationRepository, applicationWorkflowService, courseService,
                academicYearService, batchService, auditLogWriter);
        Application application = buildApplication(buildCourse(), buildYear(), BigDecimal.valueOf(90));
        application.setStatus(ApplicationStatus.SUBMITTED);
        when(applicationWorkflowService.getApplicationEntity(application.getId())).thenReturn(application);

        OfferSeatRequest request = new OfferSeatRequest();

        // ACT / ASSERT
        assertThatThrownBy(() -> meritProcessingService.offerSeat(application.getId(), request))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("offerSeat() — a MERIT_LISTED application is offered a seat and transitions to SEAT_ALLOTTED")
    void offerSeat_meritListed_succeeds() {
        // ARRANGE
        meritProcessingService = new MeritProcessingService(meritListRepository, meritListEntryRepository,
                seatAllotmentRepository, applicationRepository, applicationWorkflowService, courseService,
                academicYearService, batchService, auditLogWriter);
        Application application = buildApplication(buildCourse(), buildYear(), BigDecimal.valueOf(90));
        application.setStatus(ApplicationStatus.MERIT_LISTED);
        when(applicationWorkflowService.getApplicationEntity(application.getId())).thenReturn(application);
        when(seatAllotmentRepository.save(any(SeatAllotment.class))).thenAnswer(inv -> {
            SeatAllotment allotment = inv.getArgument(0);
            allotment.setId(UUID.randomUUID());
            return allotment;
        });
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        Batch batch = Batch.builder().build();
        batch.setId(UUID.randomUUID());
        when(batchService.getBatchEntity(batch.getId())).thenReturn(batch);

        OfferSeatRequest request = new OfferSeatRequest();
        request.setBatchId(batch.getId());

        // ACT
        SeatAllotmentDto dto = meritProcessingService.offerSeat(application.getId(), request);

        // ASSERT
        assertThat(dto.getStatus()).isEqualTo(SeatAllotmentStatus.OFFERED);
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.SEAT_ALLOTTED);
    }

    @Test
    @DisplayName("getAcceptedSeatAllotmentEntity() — an OFFERED (not yet accepted) allotment is rejected")
    void getAcceptedSeatAllotmentEntity_notAccepted_throws() {
        // ARRANGE
        meritProcessingService = new MeritProcessingService(meritListRepository, meritListEntryRepository,
                seatAllotmentRepository, applicationRepository, applicationWorkflowService, courseService,
                academicYearService, batchService, auditLogWriter);
        Application application = buildApplication(buildCourse(), buildYear(), BigDecimal.valueOf(90));
        SeatAllotment allotment = SeatAllotment.builder().application(application).course(application.getCourse())
                .status(SeatAllotmentStatus.OFFERED).build();
        when(seatAllotmentRepository.findByApplicationIdAndDeletedFalse(application.getId()))
                .thenReturn(Optional.of(allotment));

        // ACT / ASSERT
        assertThatThrownBy(() -> meritProcessingService.getAcceptedSeatAllotmentEntity(application.getId()))
                .isInstanceOf(BusinessRuleViolationException.class);
    }
}
