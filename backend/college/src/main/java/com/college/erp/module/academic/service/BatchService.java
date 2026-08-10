package com.college.erp.module.academic.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.BusinessRuleViolationException;
import com.college.erp.common.exception.DuplicateResourceException;
import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.module.academic.domain.AcademicYear;
import com.college.erp.module.academic.domain.AcademicYearRepository;
import com.college.erp.module.academic.domain.Batch;
import com.college.erp.module.academic.domain.BatchRepository;
import com.college.erp.module.academic.domain.Course;
import com.college.erp.module.academic.domain.CourseRepository;
import com.college.erp.module.academic.domain.Section;
import com.college.erp.module.academic.domain.SectionRepository;
import com.college.erp.module.academic.dto.BatchDto;
import com.college.erp.module.academic.dto.CreateBatchRequest;
import com.college.erp.module.academic.dto.CreateSectionRequest;
import com.college.erp.module.academic.dto.SectionDto;
import com.college.erp.tenant.TenantContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BatchService {

    private static final String MODULE = "ACADEMIC";

    /**
     * 05-api-documentation.md §3 Batches only says promote-term can 422 "Already at final term" —
     * it doesn't state terms-per-year. Assuming a semester system (2 terms/year), matching the
     * "Semester I" example term name in 04-database-schema.md §6.4. Revisit if that's wrong.
     */
    private static final int TERMS_PER_YEAR = 2;

    private final BatchRepository batchRepository;
    private final CourseRepository courseRepository;
    private final AcademicYearRepository academicYearRepository;
    private final SectionRepository sectionRepository;
    private final AuditLogWriter auditLogWriter;

    @Transactional
    public BatchDto create(CreateBatchRequest request) {
        Course course = courseRepository.findByIdAndDeletedFalse(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course", request.getCourseId()));
        AcademicYear academicYear = academicYearRepository.findByIdAndDeletedFalse(request.getAcademicYearId())
                .orElseThrow(() -> new ResourceNotFoundException("AcademicYear", request.getAcademicYearId()));

        if (batchRepository.existsByCourseIdAndAcademicYearIdAndDeletedFalse(course.getId(), academicYear.getId())) {
            throw new DuplicateResourceException(
                    "A batch for this course already exists in academic year " + academicYear.getLabel());
        }

        Batch batch = Batch.builder()
                .course(course)
                .academicYear(academicYear)
                .label(request.getLabel())
                .intakeYear(request.getIntakeYear())
                .build();
        batch.setTenantId(TenantContext.getCurrentTenantId());

        Batch saved = batchRepository.save(batch);
        auditLogWriter.write(AuditAction.CREATE, MODULE, "Batch", saved.getId().toString(),
                "Created: " + saved.getLabel());

        return BatchDto.from(saved);
    }

    @Transactional
    public void promoteTerm(UUID id) {
        Batch batch = getEntity(id);
        int maxTerms = batch.getCourse().getDurationYears() * TERMS_PER_YEAR;

        if (batch.getCurrentTermNumber() >= maxTerms) {
            throw new BusinessRuleViolationException("Batch is already at its final term");
        }

        batch.promoteTerm();
        batchRepository.save(batch);

        auditLogWriter.write(AuditAction.UPDATE, MODULE, "Batch", id.toString(),
                "Promoted to term " + batch.getCurrentTermNumber());
    }

    @Transactional
    public SectionDto addSection(UUID batchId, CreateSectionRequest request) {
        Batch batch = getEntity(batchId);

        if (sectionRepository.existsByBatchIdAndNameAndDeletedFalse(batchId, request.getName())) {
            throw new DuplicateResourceException(
                    "Section '" + request.getName() + "' already exists in this batch");
        }

        Section section = Section.builder()
                .batch(batch)
                .name(request.getName())
                .capacity(request.getCapacity())
                .build();
        section.setTenantId(TenantContext.getCurrentTenantId());

        Section saved = sectionRepository.save(section);
        auditLogWriter.write(AuditAction.CREATE, MODULE, "Section", saved.getId().toString(),
                "Created section '" + saved.getName() + "' in batch " + batch.getLabel());

        return SectionDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<SectionDto> listSections(UUID batchId) {
        return sectionRepository.findByBatchIdAndDeletedFalse(batchId).stream()
                .map(SectionDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public BatchDto getById(UUID id) {
        return BatchDto.from(getEntity(id));
    }

    @Transactional(readOnly = true)
    public List<BatchDto> listByCourse(UUID courseId) {
        return batchRepository.findByCourseIdAndDeletedFalse(courseId).stream()
                .map(BatchDto::from)
                .toList();
    }

    /**
     * For other modules that need the managed entity itself (e.g. module.student's Enrollment
     * needs an actual Batch to set a @ManyToOne on) — the cross-module rule is "go through the
     * service, never the repository" (07-coding-standards.md §4.4), not "services may only return
     * DTOs"; that section's own example has FeeCollectionService call studentService.getById(...)
     * and get back a Student entity.
     */
    @Transactional(readOnly = true)
    public Batch getBatchEntity(UUID id) {
        return getEntity(id);
    }

    private Batch getEntity(UUID id) {
        return batchRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Batch", id));
    }
}
