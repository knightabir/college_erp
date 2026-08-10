package com.college.erp.module.examination.service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.AccessDeniedException;
import com.college.erp.common.exception.BusinessRuleViolationException;
import com.college.erp.module.academic.domain.Subject;
import com.college.erp.module.academic.domain.SubjectOffering;
import com.college.erp.module.academic.domain.SubjectType;
import com.college.erp.module.academic.service.SubjectService;
import com.college.erp.module.examination.domain.Exam;
import com.college.erp.module.examination.domain.MarksEntry;
import com.college.erp.module.examination.domain.MarksEntryRepository;
import com.college.erp.module.examination.domain.MarksStatus;
import com.college.erp.module.examination.dto.EnterMarksRequest;
import com.college.erp.module.examination.dto.MarksEntryDto;
import com.college.erp.module.student.domain.Student;
import com.college.erp.module.student.service.StudentService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarksEntryServiceTest {

    @Mock
    private MarksEntryRepository marksEntryRepository;
    @Mock
    private ExamService examService;
    @Mock
    private SubjectService subjectService;
    @Mock
    private StudentService studentService;
    @Mock
    private AuditLogWriter auditLogWriter;

    private MarksEntryService marksEntryService;

    private Exam buildExam() {
        Exam exam = Exam.builder().name("End Term").build();
        exam.setId(UUID.randomUUID());
        return exam;
    }

    private SubjectOffering buildOffering() {
        Subject subject = Subject.builder().code("CS301").name("Data Structures")
                .type(SubjectType.THEORY).build();
        SubjectOffering offering = SubjectOffering.builder().subject(subject).build();
        offering.setId(UUID.randomUUID());
        return offering;
    }

    private Student buildStudent() {
        Student student = Student.builder().enrollmentNumber("2024CSE001").firstName("Arjun")
                .lastName("Kumar").build();
        student.setId(UUID.randomUUID());
        return student;
    }

    private EnterMarksRequest buildRequest(Exam exam, SubjectOffering offering, Student student) {
        EnterMarksRequest request = new EnterMarksRequest();
        request.setExamId(exam.getId());
        request.setOfferingId(offering.getId());
        request.setStudentId(student.getId());
        request.setInternalMarks(BigDecimal.valueOf(36.5));
        request.setExternalMarks(BigDecimal.valueOf(54.0));
        return request;
    }

    @Test
    @DisplayName("enter() — first entry is created in DRAFT with total_marks computed")
    void enter_newEntry_createsDraftWithTotal() {
        // ARRANGE
        marksEntryService = new MarksEntryService(marksEntryRepository, examService, subjectService,
                studentService, auditLogWriter);
        Exam exam = buildExam();
        SubjectOffering offering = buildOffering();
        Student student = buildStudent();
        UUID facultyId = UUID.randomUUID();

        when(examService.getEntity(exam.getId())).thenReturn(exam);
        when(subjectService.getOfferingEntity(offering.getId())).thenReturn(offering);
        when(studentService.getStudentEntity(student.getId())).thenReturn(student);
        when(marksEntryRepository.findByExamIdAndOfferingIdAndStudentIdAndDeletedFalse(
                exam.getId(), offering.getId(), student.getId())).thenReturn(Optional.empty());
        when(marksEntryRepository.save(any(MarksEntry.class))).thenAnswer(inv -> {
            MarksEntry m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });

        // ACT
        MarksEntryDto result = marksEntryService.enter(buildRequest(exam, offering, student), facultyId);

        // ASSERT
        assertThat(result.getStatus()).isEqualTo(MarksStatus.DRAFT);
        assertThat(result.getTotalMarks()).isEqualByComparingTo("90.5");
    }

    @Test
    @DisplayName("enter() — re-entering while still DRAFT updates the existing row in place")
    void enter_existingDraft_updatesInPlace() {
        // ARRANGE
        marksEntryService = new MarksEntryService(marksEntryRepository, examService, subjectService,
                studentService, auditLogWriter);
        Exam exam = buildExam();
        SubjectOffering offering = buildOffering();
        Student student = buildStudent();
        UUID facultyId = UUID.randomUUID();

        MarksEntry existing = MarksEntry.builder().exam(exam).offering(offering).student(student)
                .status(MarksStatus.DRAFT).enteredBy(facultyId)
                .internalMarks(BigDecimal.valueOf(30)).externalMarks(BigDecimal.valueOf(40)).build();
        existing.setId(UUID.randomUUID());

        when(examService.getEntity(exam.getId())).thenReturn(exam);
        when(subjectService.getOfferingEntity(offering.getId())).thenReturn(offering);
        when(studentService.getStudentEntity(student.getId())).thenReturn(student);
        when(marksEntryRepository.findByExamIdAndOfferingIdAndStudentIdAndDeletedFalse(
                exam.getId(), offering.getId(), student.getId())).thenReturn(Optional.of(existing));
        when(marksEntryRepository.save(any(MarksEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        // ACT
        MarksEntryDto result = marksEntryService.enter(buildRequest(exam, offering, student), facultyId);

        // ASSERT
        assertThat(result.getId()).isEqualTo(existing.getId());
        assertThat(result.getInternalMarks()).isEqualByComparingTo("36.5");
        assertThat(result.getTotalMarks()).isEqualByComparingTo("90.5");
    }

    @Test
    @DisplayName("enter() — re-entering after SUBMITTED is rejected")
    void enter_existingSubmitted_throws() {
        // ARRANGE
        marksEntryService = new MarksEntryService(marksEntryRepository, examService, subjectService,
                studentService, auditLogWriter);
        Exam exam = buildExam();
        SubjectOffering offering = buildOffering();
        Student student = buildStudent();

        MarksEntry existing = MarksEntry.builder().exam(exam).offering(offering).student(student)
                .status(MarksStatus.SUBMITTED).build();
        existing.setId(UUID.randomUUID());

        when(examService.getEntity(exam.getId())).thenReturn(exam);
        when(subjectService.getOfferingEntity(offering.getId())).thenReturn(offering);
        when(studentService.getStudentEntity(student.getId())).thenReturn(student);
        when(marksEntryRepository.findByExamIdAndOfferingIdAndStudentIdAndDeletedFalse(
                exam.getId(), offering.getId(), student.getId())).thenReturn(Optional.of(existing));

        // ACT / ASSERT
        assertThatThrownBy(() -> marksEntryService.enter(
                buildRequest(exam, offering, student), UUID.randomUUID()))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("submit() — only the faculty who entered the marks may submit them")
    void submit_wrongFaculty_throwsAccessDenied() {
        // ARRANGE
        marksEntryService = new MarksEntryService(marksEntryRepository, examService, subjectService,
                studentService, auditLogWriter);
        UUID entryId = UUID.randomUUID();
        MarksEntry entry = MarksEntry.builder().status(MarksStatus.DRAFT).enteredBy(UUID.randomUUID()).build();
        entry.setId(entryId);
        when(marksEntryRepository.findByIdAndDeletedFalse(entryId)).thenReturn(Optional.of(entry));

        // ACT / ASSERT
        assertThatThrownBy(() -> marksEntryService.submit(entryId, UUID.randomUUID()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("submit() — DRAFT owned by the requester moves to SUBMITTED")
    void submit_ownDraft_submits() {
        // ARRANGE
        marksEntryService = new MarksEntryService(marksEntryRepository, examService, subjectService,
                studentService, auditLogWriter);
        UUID entryId = UUID.randomUUID();
        UUID facultyId = UUID.randomUUID();
        MarksEntry entry = MarksEntry.builder().exam(buildExam()).offering(buildOffering())
                .student(buildStudent()).status(MarksStatus.DRAFT).enteredBy(facultyId).build();
        entry.setId(entryId);
        when(marksEntryRepository.findByIdAndDeletedFalse(entryId)).thenReturn(Optional.of(entry));
        when(marksEntryRepository.save(any(MarksEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        // ACT
        MarksEntryDto result = marksEntryService.submit(entryId, facultyId);

        // ASSERT
        assertThat(result.getStatus()).isEqualTo(MarksStatus.SUBMITTED);
    }

    @Test
    @DisplayName("submit() — already-submitted entry cannot be submitted again")
    void submit_alreadySubmitted_throws() {
        // ARRANGE
        marksEntryService = new MarksEntryService(marksEntryRepository, examService, subjectService,
                studentService, auditLogWriter);
        UUID entryId = UUID.randomUUID();
        UUID facultyId = UUID.randomUUID();
        MarksEntry entry = MarksEntry.builder().status(MarksStatus.SUBMITTED).enteredBy(facultyId).build();
        entry.setId(entryId);
        when(marksEntryRepository.findByIdAndDeletedFalse(entryId)).thenReturn(Optional.of(entry));

        // ACT / ASSERT
        assertThatThrownBy(() -> marksEntryService.submit(entryId, facultyId))
                .isInstanceOf(BusinessRuleViolationException.class);
    }
}
