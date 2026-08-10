package com.college.erp.module.examination.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.BusinessRuleViolationException;
import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.module.academic.domain.SubjectOffering;
import com.college.erp.module.examination.domain.Exam;
import com.college.erp.module.examination.domain.MarksEntry;
import com.college.erp.module.examination.domain.MarksEntryRepository;
import com.college.erp.module.examination.domain.MarksStatus;
import com.college.erp.module.examination.domain.ResultRecord;
import com.college.erp.module.examination.domain.ResultRecordRepository;
import com.college.erp.module.examination.domain.ResultStatus;
import com.college.erp.module.examination.dto.ResultDto;
import com.college.erp.module.examination.dto.SubjectResultDto;
import com.college.erp.module.examination.service.GradingScale.GradeResult;
import com.college.erp.module.student.domain.Student;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 03-srs.md [EXAM-005]. Publishing an exam recomputes the ResultRecord for every affected
 * student's (student, term) row from ALL of that term's currently-published marks entries — not
 * just this exam's — so a term result naturally combines contributions as multiple exams
 * (INTERNAL_1, INTERNAL_2, END_TERM, ...) get published incrementally. The doc doesn't spell this
 * multi-exam-per-term aggregation out explicitly, but result_records is UNIQUE per (student, term)
 * while marks_entries is UNIQUE per (exam, offering, student) — one row per term is only
 * consistent if it's recomputed this way.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResultProcessingService {

    private static final String MODULE = "EXAMINATION";

    private final MarksEntryRepository marksEntryRepository;
    private final ResultRecordRepository resultRecordRepository;
    private final ExamService examService;
    private final GradingScale gradingScale;
    private final AuditLogWriter auditLogWriter;

    @Transactional
    public int publish(UUID examId) {
        Exam exam = examService.getEntity(examId);
        List<MarksEntry> entries = marksEntryRepository.findByExamIdAndDeletedFalse(examId);

        boolean allModerated = entries.stream().allMatch(e -> e.getStatus() == MarksStatus.MODERATED);
        if (!allModerated) {
            throw new BusinessRuleViolationException("Some marks are not yet moderated");
        }

        Instant now = Instant.now();
        for (MarksEntry entry : entries) {
            gradeEntry(entry);
            entry.setStatus(MarksStatus.PUBLISHED);
        }
        marksEntryRepository.saveAll(entries);

        Set<UUID> studentIds = new LinkedHashSet<>();
        entries.forEach(e -> studentIds.add(e.getStudent().getId()));
        studentIds.forEach(studentId -> recomputeResultRecord(studentId, exam, now));

        auditLogWriter.write(AuditAction.RESULT_PUBLISHED, MODULE, "Exam", examId.toString(),
                "Published results for " + entries.size() + " marks entries, " + studentIds.size() + " students");
        log.info("Published exam [{}]: {} marks entries, {} students", exam.getName(), entries.size(),
                studentIds.size());

        return entries.size();
    }

    @Transactional(readOnly = true)
    public ResultDto getResult(UUID studentId, UUID termId) {
        ResultRecord record = resultRecordRepository.findByStudentIdAndTermIdAndDeletedFalse(studentId, termId)
                .orElseThrow(() -> new ResourceNotFoundException("Result for student in this term", studentId));

        List<SubjectResultDto> subjects = marksEntryRepository.findPublishedByStudentAndTerm(studentId, termId)
                .stream()
                .map(this::toSubjectResult)
                .toList();

        return ResultDto.builder()
                .studentId(studentId)
                .termId(termId)
                .sgpa(record.getSgpa())
                .cgpa(record.getCgpa())
                .totalCreditsEarned(record.getTotalCreditsEarned())
                .resultStatus(record.getResultStatus())
                .subjects(subjects)
                .build();
    }

    // Package-private (not private): RevaluationService reuses this exact grading logic when a
    // revaluation changes a mark, so a re-graded entry is computed identically to a first-time one.
    void gradeEntry(MarksEntry entry) {
        SubjectOffering offering = entry.getOffering();
        BigDecimal internal = orZero(entry.getInternalMarks());
        BigDecimal external = orZero(entry.getExternalMarks());
        BigDecimal total = internal.add(external);
        int maxTotal = offering.getMaxInternalMarks() + offering.getMaxExternalMarks();

        boolean pass = internal.compareTo(BigDecimal.valueOf(offering.getPassMarksInternal())) >= 0
                && external.compareTo(BigDecimal.valueOf(offering.getPassMarksExternal())) >= 0;

        BigDecimal percentage = maxTotal == 0 ? BigDecimal.ZERO
                : total.multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(maxTotal), 2, RoundingMode.HALF_UP);
        GradeResult gradeResult = pass ? gradingScale.gradeFor(percentage) : new GradeResult("F", BigDecimal.ZERO);

        entry.setTotalMarks(total);
        entry.setPass(pass);
        entry.setGrade(gradeResult.grade());
        entry.setGradePoints(gradeResult.gradePoints());
    }

    /** Recomputes SGPA (this term's published entries, credit-weighted) and CGPA (every term's
     *  ResultRecord including this one, credit-weighted) for one student. */
    // Package-private for the same reason as gradeEntry above.
    void recomputeResultRecord(UUID studentId, Exam exam, Instant publishedAt) {
        List<MarksEntry> termEntries =
                marksEntryRepository.findPublishedByStudentAndTerm(studentId, exam.getTerm().getId());
        Student student = termEntries.get(0).getStudent(); // guaranteed non-empty — this student has >=1 entry

        BigDecimal sgpaCreditSum = BigDecimal.ZERO;
        BigDecimal sgpaWeightedSum = BigDecimal.ZERO;
        int creditsEarned = 0;
        boolean anyFail = false;

        for (MarksEntry entry : termEntries) {
            int credits = entry.getOffering().getSubject().getCredits();
            sgpaCreditSum = sgpaCreditSum.add(BigDecimal.valueOf(credits));
            sgpaWeightedSum = sgpaWeightedSum.add(entry.getGradePoints().multiply(BigDecimal.valueOf(credits)));
            if (Boolean.TRUE.equals(entry.getPass())) {
                creditsEarned += credits;
            } else {
                anyFail = true;
            }
        }
        BigDecimal sgpa = sgpaCreditSum.signum() == 0 ? BigDecimal.ZERO
                : sgpaWeightedSum.divide(sgpaCreditSum, 2, RoundingMode.HALF_UP);

        ResultRecord record = resultRecordRepository
                .findByStudentIdAndTermIdAndDeletedFalse(studentId, exam.getTerm().getId())
                .orElseGet(() -> ResultRecord.builder()
                        .student(student)
                        .batch(exam.getBatch())
                        .term(exam.getTerm())
                        .build());

        record.setSgpa(sgpa);
        record.setTotalCreditsEarned(creditsEarned);
        record.setResultStatus(anyFail ? ResultStatus.FAIL : ResultStatus.PASS);
        record.setPublishedAt(publishedAt);
        record.setCgpa(computeCgpa(studentId, record));

        resultRecordRepository.save(record);
    }

    private BigDecimal computeCgpa(UUID studentId, ResultRecord currentTermRecord) {
        List<ResultRecord> allRecords = resultRecordRepository.findByStudentIdAndDeletedFalse(studentId);

        BigDecimal creditSum = BigDecimal.ZERO;
        BigDecimal weightedSum = BigDecimal.ZERO;
        boolean sawCurrentTerm = false;

        for (ResultRecord record : allRecords) {
            ResultRecord effective = record.getTerm().getId().equals(currentTermRecord.getTerm().getId())
                    ? currentTermRecord : record;
            if (effective == currentTermRecord) {
                sawCurrentTerm = true;
            }
            if (effective.getSgpa() == null || effective.getTotalCreditsEarned() == null) {
                continue;
            }
            BigDecimal credits = BigDecimal.valueOf(effective.getTotalCreditsEarned());
            creditSum = creditSum.add(credits);
            weightedSum = weightedSum.add(effective.getSgpa().multiply(credits));
        }
        if (!sawCurrentTerm) {
            BigDecimal credits = BigDecimal.valueOf(currentTermRecord.getTotalCreditsEarned());
            creditSum = creditSum.add(credits);
            weightedSum = weightedSum.add(currentTermRecord.getSgpa().multiply(credits));
        }

        return creditSum.signum() == 0 ? BigDecimal.ZERO : weightedSum.divide(creditSum, 2, RoundingMode.HALF_UP);
    }

    private SubjectResultDto toSubjectResult(MarksEntry entry) {
        return SubjectResultDto.builder()
                .subjectCode(entry.getOffering().getSubject().getCode())
                .subjectName(entry.getOffering().getSubject().getName())
                .internalMarks(entry.getInternalMarks())
                .externalMarks(entry.getExternalMarks())
                .total(entry.getTotalMarks())
                .grade(entry.getGrade())
                .gradePoints(entry.getGradePoints())
                .pass(Boolean.TRUE.equals(entry.getPass()))
                .build();
    }

    private BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
