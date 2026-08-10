package com.college.erp.module.academic.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.DuplicateResourceException;
import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.module.academic.domain.Batch;
import com.college.erp.module.academic.domain.BatchRepository;
import com.college.erp.module.academic.domain.Department;
import com.college.erp.module.academic.domain.DepartmentRepository;
import com.college.erp.module.academic.domain.Subject;
import com.college.erp.module.academic.domain.SubjectOffering;
import com.college.erp.module.academic.domain.SubjectOfferingRepository;
import com.college.erp.module.academic.domain.SubjectRepository;
import com.college.erp.module.academic.domain.Term;
import com.college.erp.module.academic.domain.TermRepository;
import com.college.erp.module.academic.dto.CreateSubjectOfferingRequest;
import com.college.erp.module.academic.dto.CreateSubjectRequest;
import com.college.erp.module.academic.dto.SubjectDto;
import com.college.erp.module.academic.dto.SubjectOfferingDto;
import com.college.erp.tenant.TenantContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SubjectService {

    private static final String MODULE = "ACADEMIC";

    private final SubjectRepository subjectRepository;
    private final DepartmentRepository departmentRepository;
    private final SubjectOfferingRepository subjectOfferingRepository;
    private final BatchRepository batchRepository;
    private final TermRepository termRepository;
    private final AuditLogWriter auditLogWriter;

    @Transactional
    public SubjectDto create(CreateSubjectRequest request) {
        Department department = departmentRepository.findByIdAndDeletedFalse(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department", request.getDepartmentId()));

        String code = request.getCode().toUpperCase();
        if (subjectRepository.existsByCodeAndDeletedFalse(code)) {
            throw new DuplicateResourceException("Subject with code '" + code + "' already exists");
        }

        Subject subject = Subject.builder()
                .code(code)
                .name(request.getName())
                .department(department)
                .credits(request.getCredits())
                .type(request.getType())
                .description(request.getDescription())
                .build();
        subject.setTenantId(TenantContext.getCurrentTenantId());

        Subject saved = subjectRepository.save(subject);
        auditLogWriter.write(AuditAction.CREATE, MODULE, "Subject", saved.getId().toString(),
                "Created: " + saved.getName());

        return SubjectDto.from(saved);
    }

    @Transactional
    public SubjectOfferingDto createOffering(CreateSubjectOfferingRequest request) {
        Batch batch = batchRepository.findByIdAndDeletedFalse(request.getBatchId())
                .orElseThrow(() -> new ResourceNotFoundException("Batch", request.getBatchId()));
        Subject subject = subjectRepository.findByIdAndDeletedFalse(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject", request.getSubjectId()));
        Term term = termRepository.findByIdAndDeletedFalse(request.getTermId())
                .orElseThrow(() -> new ResourceNotFoundException("Term", request.getTermId()));

        if (subjectOfferingRepository.existsByBatchIdAndSubjectIdAndTermIdAndDeletedFalse(
                batch.getId(), subject.getId(), term.getId())) {
            throw new DuplicateResourceException(
                    "'" + subject.getName() + "' is already offered to this batch in this term");
        }

        SubjectOffering.SubjectOfferingBuilder builder = SubjectOffering.builder()
                .batch(batch)
                .subject(subject)
                .term(term)
                .elective(request.isElective());
        if (request.getMaxInternalMarks() != null) {
            builder.maxInternalMarks(request.getMaxInternalMarks());
        }
        if (request.getMaxExternalMarks() != null) {
            builder.maxExternalMarks(request.getMaxExternalMarks());
        }
        if (request.getPassMarksInternal() != null) {
            builder.passMarksInternal(request.getPassMarksInternal());
        }
        if (request.getPassMarksExternal() != null) {
            builder.passMarksExternal(request.getPassMarksExternal());
        }

        SubjectOffering offering = builder.build();
        offering.setTenantId(TenantContext.getCurrentTenantId());
        SubjectOffering saved = subjectOfferingRepository.save(offering);
        auditLogWriter.write(AuditAction.CREATE, MODULE, "SubjectOffering", saved.getId().toString(),
                "Offered '" + subject.getName() + "' to batch " + batch.getLabel() + " in " + term.getName());

        return SubjectOfferingDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<SubjectOfferingDto> listOfferings(UUID batchId, UUID termId) {
        return subjectOfferingRepository.findByBatchIdAndTermIdAndDeletedFalse(batchId, termId).stream()
                .map(SubjectOfferingDto::from)
                .toList();
    }

    /** For other modules that need the managed entity — see BatchService.getBatchEntity javadoc.
     *  module.attendance/module.examination both hang records off a SubjectOffering. */
    @Transactional(readOnly = true)
    public SubjectOffering getOfferingEntity(UUID id) {
        return subjectOfferingRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("SubjectOffering", id));
    }

    /** For AttendanceShortfallScanJob's nightly sweep — every offering to check for defaulters.
     *  Kept here rather than the job injecting SubjectOfferingRepository directly
     *  (07-coding-standards.md §4.4: no cross-module repository access). */
    @Transactional(readOnly = true)
    public List<SubjectOffering> listAllOfferingEntities() {
        return subjectOfferingRepository.findByDeletedFalse();
    }

    @Transactional(readOnly = true)
    public SubjectDto getById(UUID id) {
        return SubjectDto.from(getEntity(id));
    }

    @Transactional(readOnly = true)
    public Page<SubjectDto> list(Pageable pageable) {
        return subjectRepository.findByDeletedFalse(pageable).map(SubjectDto::from);
    }

    @Transactional(readOnly = true)
    public List<SubjectDto> listByDepartment(UUID departmentId) {
        return subjectRepository.findByDepartmentIdAndDeletedFalse(departmentId).stream()
                .map(SubjectDto::from)
                .toList();
    }

    private Subject getEntity(UUID id) {
        return subjectRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject", id));
    }
}
