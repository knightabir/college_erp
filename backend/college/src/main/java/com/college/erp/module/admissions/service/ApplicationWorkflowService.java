package com.college.erp.module.admissions.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
import com.college.erp.module.admissions.domain.Application;
import com.college.erp.module.admissions.domain.ApplicationDocument;
import com.college.erp.module.admissions.domain.ApplicationDocumentRepository;
import com.college.erp.module.admissions.domain.ApplicationRepository;
import com.college.erp.module.admissions.domain.ApplicationStatus;
import com.college.erp.module.admissions.domain.ApplicationWorkflowStep;
import com.college.erp.module.admissions.domain.ApplicationWorkflowStepRepository;
import com.college.erp.module.admissions.domain.LateralEntryApplication;
import com.college.erp.module.admissions.domain.LateralEntryApplicationRepository;
import com.college.erp.module.admissions.dto.ApplicationDocumentDto;
import com.college.erp.module.admissions.dto.ApplicationDto;
import com.college.erp.module.admissions.dto.ApplicationWorkflowStepDto;
import com.college.erp.module.admissions.dto.CreateApplicationRequest;
import com.college.erp.module.admissions.dto.CreateLateralEntryDetailsRequest;
import com.college.erp.module.admissions.dto.LateralEntryApplicationDto;
import com.college.erp.module.admissions.dto.ReviewApplicationRequest;
import com.college.erp.module.admissions.dto.UploadApplicationDocumentRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * INVENTED module — see V015__create_admissions_tables.sql's header comment for why this whole
 * module has no cited requirement to build against. apply()/review()/documents mirror the shape
 * of module.hr's LeaveRequestService and module.communication's HelpdeskService (apply →
 * single-stage review, append-only audit trail) since there's no admissions-specific requirement
 * to follow instead.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationWorkflowService {

    private static final String MODULE = "ADMISSIONS";

    private final ApplicationRepository applicationRepository;
    private final ApplicationDocumentRepository applicationDocumentRepository;
    private final ApplicationWorkflowStepRepository applicationWorkflowStepRepository;
    private final LateralEntryApplicationRepository lateralEntryApplicationRepository;
    private final CourseService courseService;
    private final AcademicYearService academicYearService;
    private final AuditLogWriter auditLogWriter;

    @Transactional
    public ApplicationDto apply(CreateApplicationRequest request) {
        Course course = courseService.getCourseEntity(request.getCourseId());
        AcademicYear academicYear = academicYearService.getAcademicYearEntity(request.getAcademicYearId());

        Application application = Application.builder()
                .applicationNumber(generateApplicationNumber())
                .applicantName(request.getApplicantName())
                .applicantEmail(request.getApplicantEmail())
                .applicantPhone(request.getApplicantPhone())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .course(course)
                .academicYear(academicYear)
                .category(request.getCategory())
                .qualifyingExamScore(request.getQualifyingExamScore())
                .qualifyingExamDetails(request.getQualifyingExamDetails())
                .status(ApplicationStatus.SUBMITTED)
                .build();
        Application saved = applicationRepository.save(application);

        auditLogWriter.write(AuditAction.CREATE, MODULE, "Application", saved.getId().toString(),
                "Application submitted: " + saved.getApplicationNumber() + " for " + course.getName());
        log.info("Application submitted: [{}] {}", saved.getApplicationNumber(), saved.getApplicantName());

        return ApplicationDto.from(saved);
    }

    /** Single-stage review — any transition an Admissions Officer/Admin makes is recorded as one
     *  ApplicationWorkflowStep row, same "no cited two-stage chain" reasoning as module.hr's
     *  LeaveRequestService. */
    @Transactional
    public ApplicationDto review(UUID id, ReviewApplicationRequest request, UUID reviewerUserId) {
        Application application = getEntity(id);
        ApplicationStatus previousStatus = application.getStatus();

        if (previousStatus == ApplicationStatus.ADMITTED || previousStatus == ApplicationStatus.WITHDRAWN) {
            throw new BusinessRuleViolationException(
                    "Application is already " + previousStatus + " — no further review possible");
        }

        application.transitionTo(request.getStatus());
        Application saved = applicationRepository.save(application);

        ApplicationWorkflowStep step = ApplicationWorkflowStep.builder()
                .application(saved)
                .fromStatus(previousStatus)
                .toStatus(request.getStatus())
                .remarks(request.getRemarks())
                .actionedBy(reviewerUserId)
                .build();
        applicationWorkflowStepRepository.save(step);

        auditLogWriter.write(AuditAction.UPDATE, MODULE, "Application", id.toString(),
                previousStatus + " -> " + request.getStatus() + ": " + request.getRemarks());

        return ApplicationDto.from(saved);
    }

    @Transactional(readOnly = true)
    public ApplicationDto getById(UUID id) {
        return ApplicationDto.from(getEntity(id));
    }

    @Transactional(readOnly = true)
    public Page<ApplicationDto> listByCourseAndYear(UUID courseId, UUID academicYearId, Pageable pageable) {
        return applicationRepository.findByCourseIdAndAcademicYearIdAndDeletedFalse(courseId, academicYearId, pageable)
                .map(ApplicationDto::from);
    }

    @Transactional(readOnly = true)
    public Page<ApplicationDto> listByStatus(ApplicationStatus status, Pageable pageable) {
        return applicationRepository.findByStatusAndDeletedFalse(status, pageable).map(ApplicationDto::from);
    }

    @Transactional(readOnly = true)
    public List<ApplicationWorkflowStepDto> getHistory(UUID applicationId) {
        return applicationWorkflowStepRepository.findByApplicationIdOrderByActionedAtAsc(applicationId).stream()
                .map(ApplicationWorkflowStepDto::from)
                .toList();
    }

    /** Records a fileId a prior module.storage upload produced — matching
     *  module.student's StudentDocumentController pattern (not a multipart handler itself). */
    @Transactional
    public ApplicationDocumentDto uploadDocument(UUID applicationId, UploadApplicationDocumentRequest request) {
        Application application = getEntity(applicationId);

        ApplicationDocument document = ApplicationDocument.builder()
                .application(application)
                .documentType(request.getDocumentType())
                .fileId(request.getFileId())
                .build();
        ApplicationDocument saved = applicationDocumentRepository.save(document);

        auditLogWriter.write(AuditAction.CREATE, MODULE, "ApplicationDocument", saved.getId().toString(),
                "Uploaded " + request.getDocumentType() + " for application " + applicationId);

        return ApplicationDocumentDto.from(saved);
    }

    @Transactional
    public ApplicationDocumentDto verifyDocument(UUID documentId, UUID verifierUserId, boolean approve) {
        ApplicationDocument document = applicationDocumentRepository.findById(documentId)
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("ApplicationDocument", documentId));

        if (approve) {
            document.verify(verifierUserId);
        } else {
            document.reject(verifierUserId);
        }
        ApplicationDocument saved = applicationDocumentRepository.save(document);

        auditLogWriter.write(AuditAction.UPDATE, MODULE, "ApplicationDocument", documentId.toString(),
                (approve ? "Verified" : "Rejected") + " document");

        return ApplicationDocumentDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ApplicationDocumentDto> listDocuments(UUID applicationId) {
        return applicationDocumentRepository.findByApplicationIdAndDeletedFalse(applicationId).stream()
                .map(ApplicationDocumentDto::from)
                .toList();
    }

    @Transactional
    public LateralEntryApplicationDto recordLateralEntryDetails(
            UUID applicationId, CreateLateralEntryDetailsRequest request) {
        Application application = getEntity(applicationId);

        LateralEntryApplication lateralEntry = LateralEntryApplication.builder()
                .application(application)
                .diplomaInstitution(request.getDiplomaInstitution())
                .diplomaScore(request.getDiplomaScore())
                .entryYearNumber(request.getEntryYearNumber())
                .build();
        LateralEntryApplication saved = lateralEntryApplicationRepository.save(lateralEntry);

        auditLogWriter.write(AuditAction.CREATE, MODULE, "LateralEntryApplication", saved.getId().toString(),
                "Recorded lateral entry details for application " + applicationId);

        return LateralEntryApplicationDto.from(saved);
    }

    /** For MeritProcessingService/EnrollmentConversionService — see BatchService.getBatchEntity javadoc. */
    @Transactional(readOnly = true)
    public Application getApplicationEntity(UUID id) {
        return getEntity(id);
    }

    private Application getEntity(UUID id) {
        return applicationRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application", id));
    }

    /** {2-letter-year}{5-digit-sequence}, e.g. "26-00001" — count()+1-based, same acceptable
     *  race-condition tradeoff as module.library's accession numbers at this system's scale. */
    private String generateApplicationNumber() {
        String yearPrefix = String.valueOf(LocalDate.now().getYear()).substring(2);
        long sequence = applicationRepository.countByDeletedFalse() + 1;
        String candidate = yearPrefix + "-" + String.format("%05d", sequence);
        while (applicationRepository.existsByApplicationNumber(candidate)) {
            sequence++;
            candidate = yearPrefix + "-" + String.format("%05d", sequence);
        }
        return candidate;
    }
}
