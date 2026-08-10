package com.college.erp.module.placement.service;

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
import com.college.erp.module.academic.service.AcademicYearService;
import com.college.erp.module.academic.service.CourseService;
import com.college.erp.module.placement.domain.Company;
import com.college.erp.module.placement.domain.CompanyRepository;
import com.college.erp.module.placement.domain.DriveApplication;
import com.college.erp.module.placement.domain.DriveApplicationRepository;
import com.college.erp.module.placement.domain.DriveApplicationStatus;
import com.college.erp.module.placement.domain.InterviewResult;
import com.college.erp.module.placement.domain.InterviewRound;
import com.college.erp.module.placement.domain.InterviewRoundRepository;
import com.college.erp.module.placement.domain.PlacementDrive;
import com.college.erp.module.placement.domain.PlacementDriveRepository;
import com.college.erp.module.placement.domain.PlacementOfferRepository;
import com.college.erp.module.placement.dto.CreatePlacementDriveRequest;
import com.college.erp.module.placement.dto.DriveApplicationDto;
import com.college.erp.module.placement.dto.InterviewRoundDto;
import com.college.erp.module.placement.dto.PlacementDriveDto;
import com.college.erp.module.placement.dto.RecordInterviewResultRequest;
import com.college.erp.module.placement.dto.ScheduleInterviewRoundRequest;
import com.college.erp.module.student.domain.Student;
import com.college.erp.module.student.service.StudentService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** INVENTED module — see V016__create_placement_tables.sql's header comment. */
@ExtendWith(MockitoExtension.class)
class PlacementDriveServiceTest {

    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private PlacementDriveRepository placementDriveRepository;
    @Mock
    private DriveApplicationRepository driveApplicationRepository;
    @Mock
    private InterviewRoundRepository interviewRoundRepository;
    @Mock
    private PlacementOfferRepository placementOfferRepository;
    @Mock
    private CourseService courseService;
    @Mock
    private AcademicYearService academicYearService;
    @Mock
    private StudentService studentService;
    @Mock
    private AuditLogWriter auditLogWriter;

    private PlacementDriveService placementDriveService;

    private Company buildCompany() {
        Company company = Company.builder().name("Acme Corp").build();
        company.setId(UUID.randomUUID());
        return company;
    }

    private PlacementDrive buildDrive(Company company) {
        AcademicYear year = AcademicYear.builder().label("2026-2027").build();
        year.setId(UUID.randomUUID());
        PlacementDrive drive = PlacementDrive.builder().company(company).academicYear(year)
                .title("SDE Intern").build();
        drive.setId(UUID.randomUUID());
        return drive;
    }

    private Student buildStudent() {
        Student student = Student.builder().enrollmentNumber("2024CSE001").firstName("Arjun")
                .lastName("Kumar").build();
        student.setId(UUID.randomUUID());
        return student;
    }

    private PlacementDriveService service() {
        return new PlacementDriveService(companyRepository, placementDriveRepository, driveApplicationRepository,
                interviewRoundRepository, placementOfferRepository, courseService, academicYearService,
                studentService, auditLogWriter);
    }

    @Test
    @DisplayName("createDrive() — schedules a new drive against a company")
    void createDrive_schedulesDrive() {
        // ARRANGE
        placementDriveService = service();
        Company company = buildCompany();
        AcademicYear year = AcademicYear.builder().label("2026-2027").build();
        year.setId(UUID.randomUUID());
        when(companyRepository.findByIdAndDeletedFalse(company.getId())).thenReturn(Optional.of(company));
        when(academicYearService.getAcademicYearEntity(year.getId())).thenReturn(year);
        when(placementDriveRepository.save(any(PlacementDrive.class))).thenAnswer(inv -> {
            PlacementDrive drive = inv.getArgument(0);
            drive.setId(UUID.randomUUID());
            return drive;
        });

        CreatePlacementDriveRequest request = new CreatePlacementDriveRequest();
        request.setCompanyId(company.getId());
        request.setAcademicYearId(year.getId());
        request.setTitle("SDE Intern");

        // ACT
        PlacementDriveDto dto = placementDriveService.createDrive(request);

        // ASSERT
        assertThat(dto.getTitle()).isEqualTo("SDE Intern");
        assertThat(dto.getCompanyName()).isEqualTo("Acme Corp");
    }

    @Test
    @DisplayName("apply() — a student may only apply once to the same drive")
    void apply_duplicateApplication_throws() {
        // ARRANGE
        placementDriveService = service();
        Company company = buildCompany();
        PlacementDrive drive = buildDrive(company);
        Student student = buildStudent();
        when(placementDriveRepository.findByIdAndDeletedFalse(drive.getId())).thenReturn(Optional.of(drive));
        when(driveApplicationRepository.existsByDriveIdAndStudentIdAndDeletedFalse(drive.getId(), student.getId()))
                .thenReturn(true);

        // ACT / ASSERT
        assertThatThrownBy(() -> placementDriveService.apply(drive.getId(), student.getId()))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("apply() — a first-time application succeeds with status APPLIED")
    void apply_firstTime_succeeds() {
        // ARRANGE
        placementDriveService = service();
        Company company = buildCompany();
        PlacementDrive drive = buildDrive(company);
        Student student = buildStudent();
        when(placementDriveRepository.findByIdAndDeletedFalse(drive.getId())).thenReturn(Optional.of(drive));
        when(driveApplicationRepository.existsByDriveIdAndStudentIdAndDeletedFalse(drive.getId(), student.getId()))
                .thenReturn(false);
        when(studentService.getStudentEntity(student.getId())).thenReturn(student);
        when(driveApplicationRepository.save(any(DriveApplication.class))).thenAnswer(inv -> {
            DriveApplication application = inv.getArgument(0);
            application.setId(UUID.randomUUID());
            return application;
        });

        // ACT
        DriveApplicationDto dto = placementDriveService.apply(drive.getId(), student.getId());

        // ASSERT
        assertThat(dto.getStatus()).isEqualTo(DriveApplicationStatus.APPLIED);
    }

    @Test
    @DisplayName("scheduleInterviewRound() — auto-increments the round number and moves the "
            + "application to INTERVIEWING")
    void scheduleInterviewRound_incrementsRoundNumber() {
        // ARRANGE
        placementDriveService = service();
        DriveApplication application = DriveApplication.builder().drive(buildDrive(buildCompany()))
                .student(buildStudent()).status(DriveApplicationStatus.APPLIED).build();
        UUID applicationId = UUID.randomUUID();
        application.setId(applicationId);
        when(driveApplicationRepository.findByIdAndDeletedFalse(applicationId)).thenReturn(Optional.of(application));
        when(interviewRoundRepository.countByDriveApplicationIdAndDeletedFalse(applicationId)).thenReturn(1L);
        when(interviewRoundRepository.save(any(InterviewRound.class))).thenAnswer(inv -> {
            InterviewRound round = inv.getArgument(0);
            round.setId(UUID.randomUUID());
            return round;
        });
        when(driveApplicationRepository.save(any(DriveApplication.class))).thenAnswer(inv -> inv.getArgument(0));

        ScheduleInterviewRoundRequest request = new ScheduleInterviewRoundRequest();
        request.setRoundType("TECHNICAL");

        // ACT
        InterviewRoundDto dto = placementDriveService.scheduleInterviewRound(applicationId, request);

        // ASSERT
        assertThat(dto.getRoundNumber()).isEqualTo(2);
        assertThat(application.getStatus()).isEqualTo(DriveApplicationStatus.INTERVIEWING);
    }

    @Test
    @DisplayName("recordInterviewResult() — a REJECTED result also rejects the drive application")
    void recordInterviewResult_rejected_rejectsApplication() {
        // ARRANGE
        placementDriveService = service();
        DriveApplication application = DriveApplication.builder().drive(buildDrive(buildCompany()))
                .student(buildStudent()).status(DriveApplicationStatus.INTERVIEWING).build();
        application.setId(UUID.randomUUID());
        InterviewRound round = InterviewRound.builder().driveApplication(application).roundNumber(1).build();
        UUID roundId = UUID.randomUUID();
        round.setId(roundId);
        when(interviewRoundRepository.findByIdAndDeletedFalse(roundId)).thenReturn(Optional.of(round));
        when(interviewRoundRepository.save(any(InterviewRound.class))).thenAnswer(inv -> inv.getArgument(0));
        when(driveApplicationRepository.save(any(DriveApplication.class))).thenAnswer(inv -> inv.getArgument(0));

        RecordInterviewResultRequest request = new RecordInterviewResultRequest();
        request.setResult(InterviewResult.REJECTED);
        request.setFeedback("Did not clear technical round");

        // ACT
        placementDriveService.recordInterviewResult(roundId, request);

        // ASSERT
        assertThat(application.getStatus()).isEqualTo(DriveApplicationStatus.REJECTED);
    }
}
