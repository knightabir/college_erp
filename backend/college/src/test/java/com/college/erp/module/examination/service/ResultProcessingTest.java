package com.college.erp.module.examination.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.BusinessRuleViolationException;
import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.module.academic.domain.Batch;
import com.college.erp.module.academic.domain.Subject;
import com.college.erp.module.academic.domain.SubjectOffering;
import com.college.erp.module.academic.domain.SubjectType;
import com.college.erp.module.academic.domain.Term;
import com.college.erp.module.examination.domain.Exam;
import com.college.erp.module.examination.domain.MarksEntry;
import com.college.erp.module.examination.domain.MarksEntryRepository;
import com.college.erp.module.examination.domain.MarksStatus;
import com.college.erp.module.examination.domain.ResultRecord;
import com.college.erp.module.examination.domain.ResultRecordRepository;
import com.college.erp.module.examination.domain.ResultStatus;
import com.college.erp.module.student.domain.Student;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResultProcessingTest {

    @Mock
    private MarksEntryRepository marksEntryRepository;
    @Mock
    private ResultRecordRepository resultRecordRepository;
    @Mock
    private ExamService examService;
    @Mock
    private AuditLogWriter auditLogWriter;

    private final GradingScale gradingScale = new GradingScale();
    private ResultProcessingService resultProcessingService;

    private Term buildTerm() {
        Term term = Term.builder().name("Semester I").termNumber(1).build();
        term.setId(UUID.randomUUID());
        return term;
    }

    private Exam buildExam(Term term) {
        Batch batch = Batch.builder().label("CSE 2024").intakeYear(2024).build();
        batch.setId(UUID.randomUUID());
        Exam exam = Exam.builder().name("End Term").batch(batch).term(term).build();
        exam.setId(UUID.randomUUID());
        return exam;
    }

    private Student buildStudent() {
        Student student = Student.builder().enrollmentNumber("2024CSE001").firstName("Arjun")
                .lastName("Kumar").build();
        student.setId(UUID.randomUUID());
        return student;
    }

    private MarksEntry buildEntry(Exam exam, Student student, BigDecimal internal, BigDecimal external,
            int credits, MarksStatus status) {
        Subject subject = Subject.builder().code("CS301").name("Data Structures")
                .type(SubjectType.THEORY).credits(credits).build();
        SubjectOffering offering = SubjectOffering.builder().subject(subject).term(exam.getTerm()).build();
        offering.setId(UUID.randomUUID());

        MarksEntry entry = MarksEntry.builder()
                .exam(exam).offering(offering).student(student)
                .internalMarks(internal).externalMarks(external).status(status).build();
        entry.setId(UUID.randomUUID());
        return entry;
    }

    @Test
    @DisplayName("publish() — any entry not yet MODERATED blocks the whole exam")
    void publish_notAllModerated_throws() {
        // ARRANGE
        resultProcessingService = new ResultProcessingService(marksEntryRepository, resultRecordRepository,
                examService, gradingScale, auditLogWriter);
        Term term = buildTerm();
        Exam exam = buildExam(term);
        Student student = buildStudent();
        MarksEntry entry = buildEntry(exam, student, BigDecimal.valueOf(30), BigDecimal.valueOf(40), 4,
                MarksStatus.SUBMITTED);

        when(examService.getEntity(exam.getId())).thenReturn(exam);
        when(marksEntryRepository.findByExamIdAndDeletedFalse(exam.getId())).thenReturn(List.of(entry));

        // ACT / ASSERT
        assertThatThrownBy(() -> resultProcessingService.publish(exam.getId()))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("publish() — matches the worked example in 05-api-documentation.md §6 (90 total → O / 10.0, PASS)")
    void publish_allModerated_gradesAndCreatesResultRecord() {
        // ARRANGE — internal 36 + external 54 = 90/100 = 90% → grade O, 10.0 points (offering
        // defaults: max 40+60=100, pass thresholds 16/24 — both cleared)
        resultProcessingService = new ResultProcessingService(marksEntryRepository, resultRecordRepository,
                examService, gradingScale, auditLogWriter);
        Term term = buildTerm();
        Exam exam = buildExam(term);
        Student student = buildStudent();
        MarksEntry entry = buildEntry(exam, student, BigDecimal.valueOf(36), BigDecimal.valueOf(54), 4,
                MarksStatus.MODERATED);

        when(examService.getEntity(exam.getId())).thenReturn(exam);
        when(marksEntryRepository.findByExamIdAndDeletedFalse(exam.getId())).thenReturn(List.of(entry));
        when(marksEntryRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(marksEntryRepository.findPublishedByStudentAndTerm(student.getId(), term.getId()))
                .thenReturn(List.of(entry));
        when(resultRecordRepository.findByStudentIdAndTermIdAndDeletedFalse(student.getId(), term.getId()))
                .thenReturn(Optional.empty());
        when(resultRecordRepository.findByStudentIdAndDeletedFalse(student.getId())).thenReturn(List.of());
        when(resultRecordRepository.save(any(ResultRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        // ACT
        int publishedCount = resultProcessingService.publish(exam.getId());

        // ASSERT
        assertThat(publishedCount).isEqualTo(1);
        assertThat(entry.getStatus()).isEqualTo(MarksStatus.PUBLISHED);
        assertThat(entry.getGrade()).isEqualTo("O");
        assertThat(entry.getGradePoints()).isEqualByComparingTo("10.00");
        assertThat(entry.getPass()).isTrue();
    }

    @Test
    @DisplayName("getResult() — no result record yet throws ResourceNotFoundException")
    void getResult_notFound_throws() {
        // ARRANGE
        resultProcessingService = new ResultProcessingService(marksEntryRepository, resultRecordRepository,
                examService, gradingScale, auditLogWriter);
        UUID studentId = UUID.randomUUID();
        UUID termId = UUID.randomUUID();
        when(resultRecordRepository.findByStudentIdAndTermIdAndDeletedFalse(studentId, termId))
                .thenReturn(Optional.empty());

        // ACT / ASSERT
        assertThatThrownBy(() -> resultProcessingService.getResult(studentId, termId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
