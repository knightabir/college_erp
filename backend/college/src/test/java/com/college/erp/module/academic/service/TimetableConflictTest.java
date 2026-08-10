package com.college.erp.module.academic.service;

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
import com.college.erp.module.academic.domain.Batch;
import com.college.erp.module.academic.domain.Course;
import com.college.erp.module.academic.domain.CourseLevel;
import com.college.erp.module.academic.domain.FacultySubjectMapping;
import com.college.erp.module.academic.domain.FacultySubjectMappingRepository;
import com.college.erp.module.academic.domain.Section;
import com.college.erp.module.academic.domain.SectionRepository;
import com.college.erp.module.academic.domain.Subject;
import com.college.erp.module.academic.domain.SubjectOffering;
import com.college.erp.module.academic.domain.SubjectOfferingRepository;
import com.college.erp.module.academic.domain.SubjectType;
import com.college.erp.module.academic.domain.Term;
import com.college.erp.module.academic.dto.AssignFacultyRequest;
import com.college.erp.module.academic.dto.TimetableEntryDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Named to match 08-system-architecture.md's test tree (TimetableConflictTest.java under
 * module/academic). Covers ConflictDetectionService directly and through TimetableService.
 */
@ExtendWith(MockitoExtension.class)
class TimetableConflictTest {

    @Mock
    private FacultySubjectMappingRepository facultySubjectMappingRepository;
    @Mock
    private SubjectOfferingRepository subjectOfferingRepository;
    @Mock
    private SectionRepository sectionRepository;
    @Mock
    private AuditLogWriter auditLogWriter;

    private ConflictDetectionService conflictDetectionService;
    private TimetableService timetableService;

    private SubjectOffering buildOffering(UUID id) {
        Course course = Course.builder().name("BTech CSE").code("BTCSE").level(CourseLevel.UG)
                .durationYears(4).intakeCapacity(60).build();
        Batch batch = Batch.builder().course(course).label("CSE 2024").intakeYear(2024).build();
        Subject subject = Subject.builder().code("CS101").name("Data Structures").type(SubjectType.THEORY).build();
        Term term = Term.builder().name("Semester I").termNumber(1).build();
        term.setId(UUID.randomUUID());
        SubjectOffering offering = SubjectOffering.builder().batch(batch).subject(subject).term(term).build();
        offering.setId(id);
        return offering;
    }

    private Section buildSection(UUID id) {
        Section section = Section.builder().name("A").capacity(60).build();
        section.setId(id);
        return section;
    }

    @Test
    @DisplayName("checkNoDuplicatePrimary() — an existing active primary mapping blocks a second one")
    void checkNoDuplicatePrimary_existingActivePrimary_throws() {
        // ARRANGE
        conflictDetectionService = new ConflictDetectionService(facultySubjectMappingRepository);
        UUID offeringId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        when(facultySubjectMappingRepository.findActivePrimaryFor(offeringId, sectionId))
                .thenReturn(List.of(FacultySubjectMapping.builder().build()));

        // ACT / ASSERT
        assertThatThrownBy(() -> conflictDetectionService.checkNoDuplicatePrimary(offeringId, sectionId))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("primary faculty");
    }

    @Test
    @DisplayName("checkNoDuplicatePrimary() — no existing mapping does not throw")
    void checkNoDuplicatePrimary_noExisting_doesNotThrow() {
        // ARRANGE
        conflictDetectionService = new ConflictDetectionService(facultySubjectMappingRepository);
        UUID offeringId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        when(facultySubjectMappingRepository.findActivePrimaryFor(offeringId, sectionId))
                .thenReturn(List.of());

        // ACT / ASSERT — no exception
        conflictDetectionService.checkNoDuplicatePrimary(offeringId, sectionId);
    }

    @Test
    @DisplayName("assignFaculty() — assigning a second primary faculty to the same section/offering is rejected")
    void assignFaculty_secondPrimaryForSameSectionAndOffering_throws() {
        // ARRANGE
        conflictDetectionService = new ConflictDetectionService(facultySubjectMappingRepository);
        timetableService = new TimetableService(facultySubjectMappingRepository, subjectOfferingRepository,
                sectionRepository, conflictDetectionService, auditLogWriter);

        UUID offeringId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        SubjectOffering offering = buildOffering(offeringId);
        Section section = buildSection(sectionId);

        when(subjectOfferingRepository.findByIdAndDeletedFalse(offeringId)).thenReturn(Optional.of(offering));
        when(sectionRepository.findByIdAndDeletedFalse(sectionId)).thenReturn(Optional.of(section));
        when(facultySubjectMappingRepository.findActivePrimaryFor(offeringId, sectionId))
                .thenReturn(List.of(FacultySubjectMapping.builder().build()));

        AssignFacultyRequest request = new AssignFacultyRequest();
        request.setOfferingId(offeringId);
        request.setSectionId(sectionId);
        request.setFacultyUserId(UUID.randomUUID());
        request.setPrimary(true);

        // ACT / ASSERT
        assertThatThrownBy(() -> timetableService.assignFaculty(request))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("assignFaculty() — a non-primary (co-teaching) assignment skips the conflict check")
    void assignFaculty_nonPrimary_succeedsEvenWithExistingPrimary() {
        // ARRANGE
        conflictDetectionService = new ConflictDetectionService(facultySubjectMappingRepository);
        timetableService = new TimetableService(facultySubjectMappingRepository, subjectOfferingRepository,
                sectionRepository, conflictDetectionService, auditLogWriter);

        UUID offeringId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        SubjectOffering offering = buildOffering(offeringId);
        Section section = buildSection(sectionId);

        when(subjectOfferingRepository.findByIdAndDeletedFalse(offeringId)).thenReturn(Optional.of(offering));
        when(sectionRepository.findByIdAndDeletedFalse(sectionId)).thenReturn(Optional.of(section));
        when(facultySubjectMappingRepository.save(any(FacultySubjectMapping.class)))
                .thenAnswer(inv -> {
                    FacultySubjectMapping m = inv.getArgument(0);
                    m.setId(UUID.randomUUID());
                    return m;
                });

        AssignFacultyRequest request = new AssignFacultyRequest();
        request.setOfferingId(offeringId);
        request.setSectionId(sectionId);
        request.setFacultyUserId(UUID.randomUUID());
        request.setPrimary(false);

        // ACT
        TimetableEntryDto result = timetableService.assignFaculty(request);

        // ASSERT
        assertThat(result.isPrimary()).isFalse();
    }
}
