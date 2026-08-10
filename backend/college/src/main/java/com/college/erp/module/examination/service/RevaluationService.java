package com.college.erp.module.examination.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.AccessDeniedException;
import com.college.erp.common.exception.BusinessRuleViolationException;
import com.college.erp.common.exception.DuplicateResourceException;
import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.module.examination.domain.MarksEntry;
import com.college.erp.module.examination.domain.MarksStatus;
import com.college.erp.module.examination.domain.RevaluationRequest;
import com.college.erp.module.examination.domain.RevaluationRequestRepository;
import com.college.erp.module.examination.domain.RevaluationStatus;
import com.college.erp.module.examination.dto.CompleteRevaluationRequest;
import com.college.erp.module.examination.dto.CreateRevaluationRequest;
import com.college.erp.module.examination.dto.RevaluationRequestDto;
import com.college.erp.module.student.domain.Student;
import com.college.erp.module.student.service.StudentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 03-srs.md [EXAM-007]. fee_payment_id is never set here — see RevaluationRequest javadoc.
 * "If marks change, an amended grade card is generated" is satisfied by re-grading the
 * MarksEntry and recomputing its ResultRecord in place; GradeCardService reads live from those,
 * so there's no separate "amended" artifact to produce.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RevaluationService {

    private static final String MODULE = "EXAMINATION";

    private final RevaluationRequestRepository revaluationRequestRepository;
    private final MarksEntryService marksEntryService;
    private final ResultProcessingService resultProcessingService;
    private final StudentService studentService;
    private final AuditLogWriter auditLogWriter;

    @Transactional
    public RevaluationRequestDto apply(UUID requestingUserId, CreateRevaluationRequest request) {
        Student student = studentService.getStudentEntityByUserId(requestingUserId);
        MarksEntry marksEntry = marksEntryService.getEntity(request.getMarksEntryId());

        if (!marksEntry.getStudent().getId().equals(student.getId())) {
            throw new AccessDeniedException("This marks entry does not belong to you");
        }
        if (marksEntry.getStatus() != MarksStatus.PUBLISHED) {
            throw new BusinessRuleViolationException(
                    "Revaluation can only be requested after the result is published");
        }
        if (revaluationRequestRepository.existsByMarksEntryIdAndDeletedFalse(marksEntry.getId())) {
            throw new DuplicateResourceException("A revaluation request already exists for this marks entry");
        }

        RevaluationRequest revaluationRequest = RevaluationRequest.builder()
                .marksEntry(marksEntry)
                .student(student)
                .reason(request.getReason())
                .status(RevaluationStatus.PENDING)
                .build();

        RevaluationRequest saved = revaluationRequestRepository.save(revaluationRequest);
        auditLogWriter.write(AuditAction.CREATE, MODULE, "RevaluationRequest", saved.getId().toString(),
                "Revaluation requested by " + student.getEnrollmentNumber());

        return RevaluationRequestDto.from(saved);
    }

    @Transactional
    public RevaluationRequestDto complete(UUID requestId, CompleteRevaluationRequest request, UUID reviewerUserId) {
        RevaluationRequest revaluationRequest = revaluationRequestRepository.findByIdAndDeletedFalse(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("RevaluationRequest", requestId));

        if (revaluationRequest.getStatus() != RevaluationStatus.PENDING) {
            throw new BusinessRuleViolationException("This revaluation request has already been reviewed");
        }

        revaluationRequest.setReviewedBy(reviewerUserId);
        revaluationRequest.setReviewedAt(Instant.now());

        if (Boolean.TRUE.equals(request.getApproved())) {
            revaluationRequest.setStatus(RevaluationStatus.COMPLETED);
            revaluationRequest.setReviewedMarks(request.getReviewedMarks());

            if (request.getReviewedMarks() != null) {
                reGrade(revaluationRequest.getMarksEntry(), request.getReviewedMarks());
            }
        } else {
            revaluationRequest.setStatus(RevaluationStatus.REJECTED);
        }

        RevaluationRequest saved = revaluationRequestRepository.save(revaluationRequest);
        auditLogWriter.write(AuditAction.UPDATE, MODULE, "RevaluationRequest", saved.getId().toString(),
                "Reviewed: " + saved.getStatus());
        log.info("Revaluation request [{}] reviewed: {}", requestId, saved.getStatus());

        return RevaluationRequestDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<RevaluationRequestDto> listByStudent(UUID studentId) {
        return revaluationRequestRepository.findByStudentIdAndDeletedFalse(studentId).stream()
                .map(RevaluationRequestDto::from)
                .toList();
    }

    /** Revaluation traditionally re-marks the external (final exam) paper, not the faculty's
     *  internal assessment — reviewedMarks replaces externalMarks. */
    private void reGrade(MarksEntry marksEntry, java.math.BigDecimal reviewedMarks) {
        marksEntry.setExternalMarks(reviewedMarks);
        resultProcessingService.gradeEntry(marksEntry);
        marksEntryService.saveGraded(marksEntry);
        resultProcessingService.recomputeResultRecord(
                marksEntry.getStudent().getId(), marksEntry.getExam(), Instant.now());
    }
}
