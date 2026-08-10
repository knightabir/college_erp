package com.college.erp.module.academic.service;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.BusinessRuleViolationException;
import com.college.erp.common.exception.DuplicateResourceException;
import com.college.erp.module.academic.domain.AcademicYear;
import com.college.erp.module.academic.domain.AcademicYearRepository;
import com.college.erp.module.academic.domain.Batch;
import com.college.erp.module.academic.domain.BatchRepository;
import com.college.erp.module.academic.domain.Course;
import com.college.erp.module.academic.domain.CourseLevel;
import com.college.erp.module.academic.domain.CourseRepository;
import com.college.erp.module.academic.domain.SectionRepository;
import com.college.erp.module.academic.dto.CreateSectionRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BatchServiceTest {

    @Mock
    private BatchRepository batchRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private AcademicYearRepository academicYearRepository;
    @Mock
    private SectionRepository sectionRepository;
    @Mock
    private AuditLogWriter auditLogWriter;

    private BatchService batchService;

    private Batch buildBatch(int currentTermNumber, int durationYears) {
        Course course = Course.builder().name("BTech CSE").code("BTCSE").level(CourseLevel.UG)
                .durationYears(durationYears).intakeCapacity(60).build();
        course.setId(UUID.randomUUID());
        AcademicYear year = AcademicYear.builder().label("2024-2025").startYear(2024).endYear(2025).build();
        year.setId(UUID.randomUUID());

        Batch batch = Batch.builder().course(course).academicYear(year).label("CSE 2024")
                .intakeYear(2024).currentTermNumber(currentTermNumber).build();
        batch.setId(UUID.randomUUID());
        return batch;
    }

    @Test
    @DisplayName("promoteTerm() — advances current_term_number by 1 when not yet at final term")
    void promoteTerm_belowFinalTerm_advances() {
        // ARRANGE — 4-year course => 8 max terms (2/year), currently at term 3
        batchService = new BatchService(batchRepository, courseRepository, academicYearRepository,
                sectionRepository, auditLogWriter);
        Batch batch = buildBatch(3, 4);
        when(batchRepository.findByIdAndDeletedFalse(batch.getId())).thenReturn(Optional.of(batch));
        when(batchRepository.save(any(Batch.class))).thenAnswer(inv -> inv.getArgument(0));

        // ACT
        batchService.promoteTerm(batch.getId());

        // ASSERT
        assertThat(batch.getCurrentTermNumber()).isEqualTo(4);
    }

    @Test
    @DisplayName("promoteTerm() — already at final term (8 for a 4-year course) throws")
    void promoteTerm_atFinalTerm_throwsBusinessRuleViolation() {
        // ARRANGE
        batchService = new BatchService(batchRepository, courseRepository, academicYearRepository,
                sectionRepository, auditLogWriter);
        Batch batch = buildBatch(8, 4);
        when(batchRepository.findByIdAndDeletedFalse(batch.getId())).thenReturn(Optional.of(batch));

        // ACT / ASSERT
        assertThatThrownBy(() -> batchService.promoteTerm(batch.getId()))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("final term");
        assertThat(batch.getCurrentTermNumber()).isEqualTo(8);
    }

    @Test
    @DisplayName("addSection() — duplicate section name within the same batch is rejected")
    void addSection_duplicateName_throws() {
        // ARRANGE
        batchService = new BatchService(batchRepository, courseRepository, academicYearRepository,
                sectionRepository, auditLogWriter);
        Batch batch = buildBatch(1, 4);
        when(batchRepository.findByIdAndDeletedFalse(batch.getId())).thenReturn(Optional.of(batch));
        when(sectionRepository.existsByBatchIdAndNameAndDeletedFalse(batch.getId(), "A")).thenReturn(true);

        CreateSectionRequest request = new CreateSectionRequest();
        request.setName("A");
        request.setCapacity(60);

        // ACT / ASSERT
        assertThatThrownBy(() -> batchService.addSection(batch.getId(), request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("create() — a second batch for the same course + academic year is rejected")
    void create_duplicateCourseAndYear_throws() {
        // ARRANGE
        batchService = new BatchService(batchRepository, courseRepository, academicYearRepository,
                sectionRepository, auditLogWriter);
        Course course = Course.builder().name("BTech CSE").code("BTCSE").level(CourseLevel.UG)
                .durationYears(4).intakeCapacity(60).build();
        UUID courseId = UUID.randomUUID();
        course.setId(courseId);
        AcademicYear year = AcademicYear.builder().label("2024-2025").startYear(2024).endYear(2025).build();
        UUID yearId = UUID.randomUUID();
        year.setId(yearId);

        when(courseRepository.findByIdAndDeletedFalse(courseId)).thenReturn(Optional.of(course));
        when(academicYearRepository.findByIdAndDeletedFalse(yearId)).thenReturn(Optional.of(year));
        when(batchRepository.existsByCourseIdAndAcademicYearIdAndDeletedFalse(courseId, yearId)).thenReturn(true);

        var request = new com.college.erp.module.academic.dto.CreateBatchRequest();
        request.setCourseId(courseId);
        request.setAcademicYearId(yearId);
        request.setLabel("CSE 2024");
        request.setIntakeYear(2024);

        // ACT / ASSERT
        assertThatThrownBy(() -> batchService.create(request))
                .isInstanceOf(DuplicateResourceException.class);
    }
}
