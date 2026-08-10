package com.college.erp.module.examination.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.AccessDeniedException;
import com.college.erp.common.exception.BusinessRuleViolationException;
import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.module.academic.domain.SubjectOffering;
import com.college.erp.module.academic.service.SubjectService;
import com.college.erp.module.examination.domain.Exam;
import com.college.erp.module.examination.domain.MarksEntry;
import com.college.erp.module.examination.domain.MarksEntryRepository;
import com.college.erp.module.examination.domain.MarksStatus;
import com.college.erp.module.examination.dto.EnterMarksRequest;
import com.college.erp.module.examination.dto.MarksEntryDto;
import com.college.erp.module.student.domain.Student;
import com.college.erp.module.student.service.StudentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 03-srs.md [EXAM-003]/[EXAM-004]. Grade/pass-fail computation is deliberately deferred to
 * ResultProcessingService at publish time — [EXAM-005] ties it to publication, not entry. Only
 * total_marks is computed eagerly (trivial arithmetic, useful for faculty to see immediately).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarksEntryService {

    private static final String MODULE = "EXAMINATION";

    private final MarksEntryRepository marksEntryRepository;
    private final ExamService examService;
    private final SubjectService subjectService;
    private final StudentService studentService;
    private final AuditLogWriter auditLogWriter;

    @Transactional
    public MarksEntryDto enter(EnterMarksRequest request, UUID enteredByUserId) {
        Exam exam = examService.getEntity(request.getExamId());
        SubjectOffering offering = subjectService.getOfferingEntity(request.getOfferingId());
        Student student = studentService.getStudentEntity(request.getStudentId());

        Optional<MarksEntry> existing = marksEntryRepository.findByExamIdAndOfferingIdAndStudentIdAndDeletedFalse(
                exam.getId(), offering.getId(), student.getId());

        MarksEntry entry;
        if (existing.isPresent()) {
            entry = existing.get();
            if (entry.getStatus() != MarksStatus.DRAFT) {
                throw new BusinessRuleViolationException(
                        "Marks already submitted for this student — cannot re-enter without a correction workflow");
            }
        } else {
            entry = MarksEntry.builder()
                    .exam(exam).offering(offering).student(student)
                    .status(MarksStatus.DRAFT)
                    .enteredBy(enteredByUserId)
                    .build();
        }

        entry.setInternalMarks(request.getInternalMarks());
        entry.setExternalMarks(request.getExternalMarks());
        entry.setTotalMarks(sumOrNull(request.getInternalMarks(), request.getExternalMarks()));

        MarksEntry saved = marksEntryRepository.save(entry);
        auditLogWriter.write(AuditAction.CREATE, MODULE, "MarksEntry", saved.getId().toString(),
                "Marks entered for " + student.getEnrollmentNumber() + " in " + offering.getSubject().getName());

        return MarksEntryDto.from(saved);
    }

    @Transactional
    public MarksEntryDto submit(UUID marksEntryId, UUID requestingFacultyUserId) {
        MarksEntry entry = getEntity(marksEntryId);

        if (entry.getEnteredBy() == null || !entry.getEnteredBy().equals(requestingFacultyUserId)) {
            throw new AccessDeniedException("Only the faculty who entered these marks may submit them");
        }
        if (entry.getStatus() != MarksStatus.DRAFT) {
            throw new BusinessRuleViolationException("Marks entry is not in DRAFT status");
        }

        entry.setStatus(MarksStatus.SUBMITTED);
        MarksEntry saved = marksEntryRepository.save(entry);

        auditLogWriter.write(AuditAction.MARKS_SUBMITTED, MODULE, "MarksEntry", marksEntryId.toString(), "Submitted");
        return MarksEntryDto.from(saved);
    }

    /** Bulk operation — moderates every SUBMITTED entry for the exam; DRAFT entries are skipped
     *  (not yet locked by faculty, so not the moderator's to approve). */
    @Transactional
    public int moderateExam(UUID examId, UUID moderatorUserId) {
        List<MarksEntry> entries = marksEntryRepository.findByExamIdAndDeletedFalse(examId).stream()
                .filter(e -> e.getStatus() == MarksStatus.SUBMITTED)
                .toList();

        Instant now = Instant.now();
        for (MarksEntry entry : entries) {
            entry.setStatus(MarksStatus.MODERATED);
            entry.setModeratedBy(moderatorUserId);
            entry.setModeratedAt(now);
        }
        marksEntryRepository.saveAll(entries);

        auditLogWriter.write(AuditAction.UPDATE, MODULE, "Exam", examId.toString(),
                "Moderated " + entries.size() + " marks entries");
        log.info("Moderated {} marks entries for exam [{}]", entries.size(), examId);

        return entries.size();
    }

    MarksEntry getEntity(UUID id) {
        return marksEntryRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("MarksEntry", id));
    }

    /** Package-private — RevaluationService re-grades an entry via ResultProcessingService.gradeEntry
     *  then persists it through here, so MarksEntryRepository stays owned by this class alone. */
    @Transactional
    void saveGraded(MarksEntry entry) {
        marksEntryRepository.save(entry);
    }

    private BigDecimal sumOrNull(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) {
            return null;
        }
        return (a == null ? BigDecimal.ZERO : a).add(b == null ? BigDecimal.ZERO : b);
    }
}
