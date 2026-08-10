package com.college.erp.module.admissions.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.BusinessRuleViolationException;
import com.college.erp.common.exception.ResourceNotFoundException;
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
import com.college.erp.module.admissions.dto.MeritListEntryDto;
import com.college.erp.module.admissions.dto.OfferSeatRequest;
import com.college.erp.module.admissions.dto.SeatAllotmentDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * INVENTED module — see V015__create_admissions_tables.sql's header comment. Ranks every
 * SHORTLISTED application for a course/year by qualifying_exam_score (descending, nulls last) —
 * there's no cited tie-break or weighting rule to follow, so this is the simplest defensible
 * reading of "merit list" given the columns actually available.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MeritProcessingService {

    private static final String MODULE = "ADMISSIONS";

    private final MeritListRepository meritListRepository;
    private final MeritListEntryRepository meritListEntryRepository;
    private final SeatAllotmentRepository seatAllotmentRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationWorkflowService applicationWorkflowService;
    private final CourseService courseService;
    private final AcademicYearService academicYearService;
    private final BatchService batchService;
    private final AuditLogWriter auditLogWriter;

    @Transactional
    public MeritListDto generate(GenerateMeritListRequest request) {
        Course course = courseService.getCourseEntity(request.getCourseId());
        AcademicYear academicYear = academicYearService.getAcademicYearEntity(request.getAcademicYearId());

        List<Application> shortlisted = applicationRepository
                .findByCourseIdAndAcademicYearIdAndStatusAndDeletedFalse(
                        course.getId(), academicYear.getId(), ApplicationStatus.SHORTLISTED);
        if (shortlisted.isEmpty()) {
            throw new BusinessRuleViolationException(
                    "No SHORTLISTED applications for this course/year to rank");
        }

        MeritList meritList = MeritList.builder()
                .course(course)
                .academicYear(academicYear)
                .name(request.getName())
                .build();
        MeritList savedList = meritListRepository.save(meritList);

        List<Application> ranked = shortlisted.stream()
                .sorted(Comparator.comparing(
                        Application::getQualifyingExamScore,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        int rank = 1;
        for (Application application : ranked) {
            MeritListEntry entry = MeritListEntry.builder()
                    .meritList(savedList)
                    .application(application)
                    .rank(rank++)
                    .score(application.getQualifyingExamScore())
                    .build();
            meritListEntryRepository.save(entry);

            application.transitionTo(ApplicationStatus.MERIT_LISTED);
            applicationRepository.save(application);
        }

        auditLogWriter.write(AuditAction.CREATE, MODULE, "MeritList", savedList.getId().toString(),
                "Generated merit list \"" + savedList.getName() + "\" with " + ranked.size() + " entries");
        log.info("Merit list generated: [{}] {} entries", savedList.getName(), ranked.size());

        return MeritListDto.from(savedList);
    }

    @Transactional
    public MeritListDto publish(UUID meritListId) {
        MeritList meritList = getMeritListEntity(meritListId);
        if (meritList.isPublished()) {
            throw new BusinessRuleViolationException("Merit list is already published");
        }
        meritList.setPublishedAt(Instant.now());
        MeritList saved = meritListRepository.save(meritList);

        auditLogWriter.write(AuditAction.UPDATE, MODULE, "MeritList", meritListId.toString(), "Published");

        return MeritListDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<MeritListEntryDto> getEntries(UUID meritListId) {
        return meritListEntryRepository.findByMeritListIdOrderByRankAsc(meritListId).stream()
                .map(MeritListEntryDto::from)
                .toList();
    }

    /** [SEAT-ALLOTMENT]: offers a seat against a MERIT_LISTED application. Batch assignment
     *  is optional at offer time (a course may not yet have next year's batches created) — see
     *  SeatAllotment's schema comment. */
    @Transactional
    public SeatAllotmentDto offerSeat(UUID applicationId, OfferSeatRequest request) {
        Application application = applicationWorkflowService.getApplicationEntity(applicationId);
        if (application.getStatus() != ApplicationStatus.MERIT_LISTED) {
            throw new BusinessRuleViolationException(
                    "Only a MERIT_LISTED application may be offered a seat");
        }
        Batch batch = request.getBatchId() != null ? batchService.getBatchEntity(request.getBatchId()) : null;

        SeatAllotment allotment = SeatAllotment.builder()
                .application(application)
                .course(application.getCourse())
                .batch(batch)
                .status(SeatAllotmentStatus.OFFERED)
                .responseDeadline(request.getResponseDeadline())
                .build();
        SeatAllotment saved = seatAllotmentRepository.save(allotment);

        application.transitionTo(ApplicationStatus.SEAT_ALLOTTED);
        applicationRepository.save(application);

        auditLogWriter.write(AuditAction.CREATE, MODULE, "SeatAllotment", saved.getId().toString(),
                "Offered a seat to application " + applicationId);

        return SeatAllotmentDto.from(saved);
    }

    @Transactional
    public SeatAllotmentDto respondToOffer(UUID seatAllotmentId, boolean accept) {
        SeatAllotment allotment = seatAllotmentRepository.findByIdAndDeletedFalse(seatAllotmentId)
                .orElseThrow(() -> new ResourceNotFoundException("SeatAllotment", seatAllotmentId));
        if (allotment.getStatus() != SeatAllotmentStatus.OFFERED) {
            throw new BusinessRuleViolationException("This seat offer already has a response");
        }

        if (accept) {
            allotment.accept();
        } else {
            allotment.decline();
        }
        SeatAllotment saved = seatAllotmentRepository.save(allotment);

        auditLogWriter.write(AuditAction.UPDATE, MODULE, "SeatAllotment", seatAllotmentId.toString(),
                accept ? "Accepted" : "Declined");

        return SeatAllotmentDto.from(saved);
    }

    /** For EnrollmentConversionService — see BatchService.getBatchEntity javadoc. */
    @Transactional(readOnly = true)
    public SeatAllotment getAcceptedSeatAllotmentEntity(UUID applicationId) {
        SeatAllotment allotment = seatAllotmentRepository.findByApplicationIdAndDeletedFalse(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("SeatAllotment for application", applicationId));
        if (allotment.getStatus() != SeatAllotmentStatus.ACCEPTED) {
            throw new BusinessRuleViolationException(
                    "Seat allotment must be ACCEPTED before the application can be converted to a student");
        }
        return allotment;
    }

    private MeritList getMeritListEntity(UUID id) {
        return meritListRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("MeritList", id));
    }
}
