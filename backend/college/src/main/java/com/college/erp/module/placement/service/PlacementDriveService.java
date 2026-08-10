package com.college.erp.module.placement.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.BusinessRuleViolationException;
import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.module.academic.domain.AcademicYear;
import com.college.erp.module.academic.domain.Course;
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
import com.college.erp.module.placement.domain.OfferStatus;
import com.college.erp.module.placement.domain.PlacementDrive;
import com.college.erp.module.placement.domain.PlacementDriveRepository;
import com.college.erp.module.placement.domain.PlacementOffer;
import com.college.erp.module.placement.domain.PlacementOfferRepository;
import com.college.erp.module.placement.dto.CompanyDto;
import com.college.erp.module.placement.dto.CreateCompanyRequest;
import com.college.erp.module.placement.dto.CreatePlacementDriveRequest;
import com.college.erp.module.placement.dto.CreatePlacementOfferRequest;
import com.college.erp.module.placement.dto.DriveApplicationDto;
import com.college.erp.module.placement.dto.InterviewRoundDto;
import com.college.erp.module.placement.dto.PlacementDriveDto;
import com.college.erp.module.placement.dto.PlacementOfferDto;
import com.college.erp.module.placement.dto.RecordInterviewResultRequest;
import com.college.erp.module.placement.dto.ScheduleInterviewRoundRequest;
import com.college.erp.module.student.domain.Student;
import com.college.erp.module.student.service.StudentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * INVENTED module — see V016__create_placement_tables.sql's header comment. Covers companies,
 * drives, student applications to a drive, interview rounds, and offers — 08-system-
 * architecture.md's file tree names only PlacementDriveService/PlacementAnalyticsService for this
 * module, so company CRUD is folded in here rather than given its own service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlacementDriveService {

    private static final String MODULE = "PLACEMENT";

    private final CompanyRepository companyRepository;
    private final PlacementDriveRepository placementDriveRepository;
    private final DriveApplicationRepository driveApplicationRepository;
    private final InterviewRoundRepository interviewRoundRepository;
    private final PlacementOfferRepository placementOfferRepository;
    private final CourseService courseService;
    private final AcademicYearService academicYearService;
    private final StudentService studentService;
    private final AuditLogWriter auditLogWriter;

    @Transactional
    public CompanyDto createCompany(CreateCompanyRequest request) {
        Company company = Company.builder()
                .name(request.getName())
                .industry(request.getIndustry())
                .website(request.getWebsite())
                .contactPerson(request.getContactPerson())
                .contactEmail(request.getContactEmail())
                .contactPhone(request.getContactPhone())
                .build();
        Company saved = companyRepository.save(company);

        auditLogWriter.write(AuditAction.CREATE, MODULE, "Company", saved.getId().toString(),
                "Registered company: " + saved.getName());

        return CompanyDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<CompanyDto> listCompanies() {
        return companyRepository.findByDeletedFalse().stream().map(CompanyDto::from).toList();
    }

    @Transactional
    public PlacementDriveDto createDrive(CreatePlacementDriveRequest request) {
        Company company = getCompanyEntity(request.getCompanyId());
        AcademicYear academicYear = academicYearService.getAcademicYearEntity(request.getAcademicYearId());
        Course eligibleCourse = request.getEligibleCourseId() != null
                ? courseService.getCourseEntity(request.getEligibleCourseId())
                : null;

        PlacementDrive drive = PlacementDrive.builder()
                .company(company)
                .academicYear(academicYear)
                .title(request.getTitle())
                .description(request.getDescription())
                .eligibleCourse(eligibleCourse)
                .eligibilityMinCgpa(request.getEligibilityMinCgpa())
                .ctcOffered(request.getCtcOffered())
                .driveDate(request.getDriveDate())
                .build();
        PlacementDrive saved = placementDriveRepository.save(drive);

        auditLogWriter.write(AuditAction.CREATE, MODULE, "PlacementDrive", saved.getId().toString(),
                "Scheduled drive \"" + saved.getTitle() + "\" for " + company.getName());
        log.info("Placement drive created: [{}] {}", company.getName(), saved.getTitle());

        return PlacementDriveDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<PlacementDriveDto> listDrivesByYear(UUID academicYearId) {
        return placementDriveRepository.findByAcademicYearIdAndDeletedFalse(academicYearId).stream()
                .map(PlacementDriveDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PlacementDriveDto getDriveById(UUID id) {
        return PlacementDriveDto.from(getDriveEntity(id));
    }

    @Transactional
    public DriveApplicationDto apply(UUID driveId, UUID studentId) {
        PlacementDrive drive = getDriveEntity(driveId);
        if (driveApplicationRepository.existsByDriveIdAndStudentIdAndDeletedFalse(driveId, studentId)) {
            throw new BusinessRuleViolationException("Student has already applied to this drive");
        }
        Student student = studentService.getStudentEntity(studentId);

        DriveApplication application = DriveApplication.builder()
                .drive(drive)
                .student(student)
                .status(DriveApplicationStatus.APPLIED)
                .build();
        DriveApplication saved = driveApplicationRepository.save(application);

        auditLogWriter.write(AuditAction.CREATE, MODULE, "DriveApplication", saved.getId().toString(),
                student.getFullName() + " applied to drive " + drive.getTitle());

        return DriveApplicationDto.from(saved);
    }

    @Transactional
    public DriveApplicationDto updateApplicationStatus(UUID driveApplicationId, DriveApplicationStatus newStatus) {
        DriveApplication application = getDriveApplicationEntity(driveApplicationId);
        application.transitionTo(newStatus);
        DriveApplication saved = driveApplicationRepository.save(application);

        auditLogWriter.write(AuditAction.UPDATE, MODULE, "DriveApplication", driveApplicationId.toString(),
                "Status -> " + newStatus);

        return DriveApplicationDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<DriveApplicationDto> listApplicationsByDrive(UUID driveId) {
        return driveApplicationRepository.findByDriveIdAndDeletedFalse(driveId).stream()
                .map(DriveApplicationDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DriveApplicationDto> listApplicationsByStudent(UUID studentId) {
        return driveApplicationRepository.findByStudentIdAndDeletedFalse(studentId).stream()
                .map(DriveApplicationDto::from)
                .toList();
    }

    @Transactional
    public InterviewRoundDto scheduleInterviewRound(UUID driveApplicationId, ScheduleInterviewRoundRequest request) {
        DriveApplication application = getDriveApplicationEntity(driveApplicationId);
        long nextRoundNumber = interviewRoundRepository.countByDriveApplicationIdAndDeletedFalse(driveApplicationId) + 1;

        InterviewRound round = InterviewRound.builder()
                .driveApplication(application)
                .roundNumber((int) nextRoundNumber)
                .roundType(request.getRoundType())
                .scheduledAt(request.getScheduledAt())
                .interviewerName(request.getInterviewerName())
                .build();
        InterviewRound saved = interviewRoundRepository.save(round);

        application.transitionTo(DriveApplicationStatus.INTERVIEWING);
        driveApplicationRepository.save(application);

        auditLogWriter.write(AuditAction.CREATE, MODULE, "InterviewRound", saved.getId().toString(),
                "Scheduled round " + nextRoundNumber + " for application " + driveApplicationId);

        return InterviewRoundDto.from(saved);
    }

    @Transactional
    public InterviewRoundDto recordInterviewResult(UUID interviewRoundId, RecordInterviewResultRequest request) {
        InterviewRound round = interviewRoundRepository.findByIdAndDeletedFalse(interviewRoundId)
                .orElseThrow(() -> new ResourceNotFoundException("InterviewRound", interviewRoundId));
        round.recordResult(request.getResult(), request.getFeedback());
        InterviewRound saved = interviewRoundRepository.save(round);

        if (request.getResult() == InterviewResult.REJECTED) {
            DriveApplication application = round.getDriveApplication();
            application.transitionTo(DriveApplicationStatus.REJECTED);
            driveApplicationRepository.save(application);
        }

        auditLogWriter.write(AuditAction.UPDATE, MODULE, "InterviewRound", interviewRoundId.toString(),
                "Result: " + request.getResult());

        return InterviewRoundDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<InterviewRoundDto> listInterviewRounds(UUID driveApplicationId) {
        return interviewRoundRepository
                .findByDriveApplicationIdAndDeletedFalseOrderByRoundNumberAsc(driveApplicationId).stream()
                .map(InterviewRoundDto::from)
                .toList();
    }

    @Transactional
    public PlacementOfferDto makeOffer(UUID driveApplicationId, CreatePlacementOfferRequest request) {
        DriveApplication application = getDriveApplicationEntity(driveApplicationId);

        PlacementOffer offer = PlacementOffer.builder()
                .driveApplication(application)
                .ctc(request.getCtc())
                .designation(request.getDesignation())
                .offerDate(request.getOfferDate())
                .joiningDate(request.getJoiningDate())
                .status(OfferStatus.OFFERED)
                .build();
        PlacementOffer saved = placementOfferRepository.save(offer);

        application.transitionTo(DriveApplicationStatus.SELECTED);
        driveApplicationRepository.save(application);

        auditLogWriter.write(AuditAction.CREATE, MODULE, "PlacementOffer", saved.getId().toString(),
                "Offered " + request.getCtc() + " to application " + driveApplicationId);

        return PlacementOfferDto.from(saved);
    }

    @Transactional
    public PlacementOfferDto respondToOffer(UUID offerId, boolean accept) {
        PlacementOffer offer = placementOfferRepository.findByIdAndDeletedFalse(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("PlacementOffer", offerId));
        if (offer.getStatus() != OfferStatus.OFFERED) {
            throw new BusinessRuleViolationException("This offer already has a response");
        }

        if (accept) {
            offer.accept();
        } else {
            offer.decline();
        }
        PlacementOffer saved = placementOfferRepository.save(offer);

        auditLogWriter.write(AuditAction.UPDATE, MODULE, "PlacementOffer", offerId.toString(),
                accept ? "Accepted" : "Declined");

        return PlacementOfferDto.from(saved);
    }

    private Company getCompanyEntity(UUID id) {
        return companyRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company", id));
    }

    private PlacementDrive getDriveEntity(UUID id) {
        return placementDriveRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("PlacementDrive", id));
    }

    private DriveApplication getDriveApplicationEntity(UUID id) {
        return driveApplicationRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("DriveApplication", id));
    }
}
