package com.college.erp.module.student.service;

import java.time.LocalDate;
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
import com.college.erp.module.academic.domain.Course;
import com.college.erp.module.academic.domain.CourseLevel;
import com.college.erp.module.academic.domain.Section;
import com.college.erp.module.academic.domain.Term;
import com.college.erp.module.academic.service.BatchService;
import com.college.erp.module.academic.service.SectionService;
import com.college.erp.module.academic.service.TermService;
import com.college.erp.module.student.domain.Enrollment;
import com.college.erp.module.student.domain.EnrollmentRepository;
import com.college.erp.module.student.domain.Student;
import com.college.erp.module.student.dto.EnrollmentDto;
import com.college.erp.module.student.dto.TransferSectionRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private BatchService batchService;
    @Mock
    private SectionService sectionService;
    @Mock
    private TermService termService;
    @Mock
    private AuditLogWriter auditLogWriter;

    private EnrollmentService enrollmentService;

    private Batch buildBatch(UUID id) {
        Course course = Course.builder().name("BTech CSE").code("BTCSE").level(CourseLevel.UG)
                .durationYears(4).intakeCapacity(60).build();
        Batch batch = Batch.builder().course(course).label("CSE 2024").intakeYear(2024)
                .currentTermNumber(1).build();
        batch.setId(id);
        return batch;
    }

    private Section buildSection(UUID id, Batch owningBatch) {
        Section section = Section.builder().batch(owningBatch).name("A").capacity(60).build();
        section.setId(id);
        return section;
    }

    private Student buildStudent() {
        Student student = Student.builder().enrollmentNumber("2024CSE001").firstName("Arjun")
                .lastName("Kumar").build();
        student.setId(UUID.randomUUID());
        return student;
    }

    @Test
    @DisplayName("createInitialEnrollment() — section belonging to the given batch succeeds")
    void createInitialEnrollment_sectionInBatch_succeeds() {
        // ARRANGE
        enrollmentService = new EnrollmentService(enrollmentRepository, batchService, sectionService,
                termService, auditLogWriter);
        UUID batchId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        Batch batch = buildBatch(batchId);
        Section section = buildSection(sectionId, batch);
        Term term = Term.builder().name("Semester I").termNumber(1).build();
        term.setId(UUID.randomUUID());
        Student student = buildStudent();

        when(batchService.getBatchEntity(batchId)).thenReturn(batch);
        when(sectionService.getSectionEntity(sectionId)).thenReturn(section);
        when(termService.getCurrentTermEntityForBatch(batch)).thenReturn(term);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> {
            Enrollment e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        // ACT
        Enrollment result = enrollmentService.createInitialEnrollment(student, batchId, sectionId);

        // ASSERT
        assertThat(result.isCurrent()).isTrue();
        assertThat(result.getSection()).isEqualTo(section);
        assertThat(result.getTerm()).isEqualTo(term);
        assertThat(result.getEnrollmentDate()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("createInitialEnrollment() — section from a different batch is rejected")
    void createInitialEnrollment_sectionInDifferentBatch_throws() {
        // ARRANGE
        enrollmentService = new EnrollmentService(enrollmentRepository, batchService, sectionService,
                termService, auditLogWriter);
        UUID batchId = UUID.randomUUID();
        UUID otherBatchId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        Batch batch = buildBatch(batchId);
        Batch otherBatch = buildBatch(otherBatchId);
        Section section = buildSection(sectionId, otherBatch);
        Student student = buildStudent();

        when(batchService.getBatchEntity(batchId)).thenReturn(batch);
        when(sectionService.getSectionEntity(sectionId)).thenReturn(section);

        // ACT / ASSERT
        assertThatThrownBy(() -> enrollmentService.createInitialEnrollment(student, batchId, sectionId))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("transferSection() — flips the old row to non-current and creates a new current one")
    void transferSection_flipsOldAndCreatesNew() {
        // ARRANGE
        enrollmentService = new EnrollmentService(enrollmentRepository, batchService, sectionService,
                termService, auditLogWriter);
        UUID batchId = UUID.randomUUID();
        Batch batch = buildBatch(batchId);
        Section oldSection = buildSection(UUID.randomUUID(), batch);
        Section newSection = buildSection(UUID.randomUUID(), batch);
        Term term = Term.builder().name("Semester I").termNumber(1).build();
        term.setId(UUID.randomUUID());
        Student student = buildStudent();

        Enrollment current = Enrollment.builder().student(student).batch(batch).section(oldSection)
                .term(term).enrollmentDate(LocalDate.now().minusMonths(2)).current(true).build();
        current.setId(UUID.randomUUID());

        when(enrollmentRepository.findByStudentIdAndCurrentTrueAndDeletedFalse(student.getId()))
                .thenReturn(Optional.of(current));
        when(sectionService.getSectionEntity(newSection.getId())).thenReturn(newSection);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> {
            Enrollment e = inv.getArgument(0);
            if (e.getId() == null) {
                e.setId(UUID.randomUUID());
            }
            return e;
        });

        TransferSectionRequest request = new TransferSectionRequest();
        request.setNewSectionId(newSection.getId());
        request.setRemarks("Schedule conflict");

        // ACT
        EnrollmentDto result = enrollmentService.transferSection(student.getId(), request);

        // ASSERT
        assertThat(current.isCurrent()).isFalse();
        assertThat(result.getSectionId()).isEqualTo(newSection.getId());
        assertThat(result.isCurrent()).isTrue();
    }

    @Test
    @DisplayName("transferSection() — no current enrollment throws ResourceNotFoundException")
    void transferSection_noCurrentEnrollment_throws() {
        // ARRANGE
        enrollmentService = new EnrollmentService(enrollmentRepository, batchService, sectionService,
                termService, auditLogWriter);
        UUID studentId = UUID.randomUUID();
        when(enrollmentRepository.findByStudentIdAndCurrentTrueAndDeletedFalse(studentId))
                .thenReturn(Optional.empty());

        TransferSectionRequest request = new TransferSectionRequest();
        request.setNewSectionId(UUID.randomUUID());

        // ACT / ASSERT
        assertThatThrownBy(() -> enrollmentService.transferSection(studentId, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
