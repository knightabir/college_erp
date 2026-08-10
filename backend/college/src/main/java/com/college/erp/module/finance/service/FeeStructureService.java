package com.college.erp.module.finance.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.DuplicateResourceException;
import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.module.academic.domain.Batch;
import com.college.erp.module.academic.domain.Course;
import com.college.erp.module.academic.service.BatchService;
import com.college.erp.module.academic.service.CourseService;
import com.college.erp.module.finance.domain.FeeHead;
import com.college.erp.module.finance.domain.FeeStructure;
import com.college.erp.module.finance.domain.FeeStructureRepository;
import com.college.erp.module.finance.dto.CreateFeeStructureRequest;
import com.college.erp.module.finance.dto.FeeStructureDto;

import lombok.RequiredArgsConstructor;

/** 03-srs.md [FIN-001], 05-api-documentation.md §7 "Fee Structure". */
@Service
@RequiredArgsConstructor
public class FeeStructureService {

    private static final String MODULE = "FINANCE";

    private final FeeStructureRepository feeStructureRepository;
    private final CourseService courseService;
    private final BatchService batchService;
    private final FeeHeadService feeHeadService;
    private final AuditLogWriter auditLogWriter;

    @Transactional
    public FeeStructureDto create(CreateFeeStructureRequest request) {
        Course course = courseService.getCourseEntity(request.getCourseId());
        Batch batch = request.getBatchId() == null ? null : batchService.getBatchEntity(request.getBatchId());
        FeeHead feeHead = feeHeadEntity(request.getFeeHeadId());

        UUID batchId = batch == null ? null : batch.getId();
        if (feeStructureRepository.existsByCourseIdAndBatchIdAndFeeHeadIdAndTermNumberAndDeletedFalse(
                course.getId(), batchId, feeHead.getId(), request.getTermNumber())) {
            throw new DuplicateResourceException(
                    "A fee structure already exists for this course/batch/fee-head/term combination");
        }

        FeeStructure structure = FeeStructure.builder()
                .course(course)
                .batch(batch)
                .feeHead(feeHead)
                .termNumber(request.getTermNumber())
                .amount(request.getAmount())
                .dueDateOffsetDays(request.getDueDateOffsetDays())
                .lateFeePerDay(request.getLateFeePerDay())
                .build();

        FeeStructure saved = feeStructureRepository.save(structure);
        auditLogWriter.write(AuditAction.CREATE, MODULE, "FeeStructure", saved.getId().toString(),
                "Created fee structure: " + feeHead.getName() + " for " + course.getName());

        return FeeStructureDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<FeeStructureDto> listByCourse(UUID courseId) {
        return feeStructureRepository.findByCourseIdAndDeletedFalse(courseId).stream()
                .map(FeeStructureDto::from)
                .toList();
    }

    /** Resolves which single structure applies per fee head for one course+batch+term: a
     *  batch-specific row overrides the course-wide (batch IS NULL) default for that fee head,
     *  never both (03-srs.md [FIN-001] — "different amounts per batch", not additive amounts). */
    List<FeeStructure> resolveApplicable(UUID courseId, UUID batchId, int termNumber) {
        List<FeeStructure> candidates = feeStructureRepository.findApplicable(courseId, batchId, termNumber);
        return candidates.stream()
                .collect(java.util.stream.Collectors.toMap(
                        fs -> fs.getFeeHead().getId(),
                        fs -> fs,
                        (courseWide, batchSpecific) -> batchSpecific.getBatch() != null ? batchSpecific : courseWide))
                .values().stream()
                .toList();
    }

    private FeeHead feeHeadEntity(UUID id) {
        return feeHeadService.getFeeHeadEntity(id);
    }
}
