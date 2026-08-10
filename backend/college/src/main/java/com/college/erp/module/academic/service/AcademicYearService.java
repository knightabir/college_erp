package com.college.erp.module.academic.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.DuplicateResourceException;
import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.module.academic.domain.AcademicYear;
import com.college.erp.module.academic.domain.AcademicYearRepository;
import com.college.erp.module.academic.dto.AcademicYearDto;
import com.college.erp.module.academic.dto.CreateAcademicYearRequest;
import com.college.erp.tenant.TenantContext;

import lombok.RequiredArgsConstructor;

/**
 * Not listed as a dedicated service/controller in 08-system-architecture.md's file tree, but
 * academic_years is a table batches/terms depend on via FK — something has to be able to create
 * one. Kept deliberately minimal, following the same conventions as DepartmentService.
 */
@Service
@RequiredArgsConstructor
public class AcademicYearService {

    private static final String MODULE = "ACADEMIC";

    private final AcademicYearRepository academicYearRepository;
    private final AuditLogWriter auditLogWriter;

    @Transactional
    public AcademicYearDto create(CreateAcademicYearRequest request) {
        if (academicYearRepository.existsByLabelAndDeletedFalse(request.getLabel())) {
            throw new DuplicateResourceException("Academic year '" + request.getLabel() + "' already exists");
        }

        AcademicYear year = AcademicYear.builder()
                .label(request.getLabel())
                .startYear(request.getStartYear())
                .endYear(request.getEndYear())
                .build();
        year.setTenantId(TenantContext.getCurrentTenantId());

        AcademicYear saved = academicYearRepository.save(year);
        auditLogWriter.write(AuditAction.CREATE, MODULE, "AcademicYear", saved.getId().toString(),
                "Created: " + saved.getLabel());

        return AcademicYearDto.from(saved);
    }

    /** Unsets whichever year is currently marked current, then marks this one — the unique
     *  partial index on is_current allows only one TRUE row at a time. */
    @Transactional
    public AcademicYearDto setCurrent(UUID id) {
        academicYearRepository.findByCurrentTrueAndDeletedFalse()
                .ifPresent(previous -> {
                    previous.setCurrent(false);
                    academicYearRepository.save(previous);
                });

        AcademicYear year = getEntity(id);
        year.setCurrent(true);
        AcademicYear saved = academicYearRepository.save(year);

        auditLogWriter.write(AuditAction.UPDATE, MODULE, "AcademicYear", saved.getId().toString(),
                "Marked current: " + saved.getLabel());

        return AcademicYearDto.from(saved);
    }

    @Transactional(readOnly = true)
    public AcademicYearDto getById(UUID id) {
        return AcademicYearDto.from(getEntity(id));
    }

    /** For other modules that need the managed entity — see BatchService.getBatchEntity javadoc. */
    @Transactional(readOnly = true)
    public AcademicYear getAcademicYearEntity(UUID id) {
        return getEntity(id);
    }

    @Transactional(readOnly = true)
    public Page<AcademicYearDto> list(Pageable pageable) {
        return academicYearRepository.findByDeletedFalse(pageable).map(AcademicYearDto::from);
    }

    private AcademicYear getEntity(UUID id) {
        return academicYearRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("AcademicYear", id));
    }
}
