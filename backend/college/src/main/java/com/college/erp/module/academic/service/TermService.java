package com.college.erp.module.academic.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.module.academic.domain.AcademicYear;
import com.college.erp.module.academic.domain.AcademicYearRepository;
import com.college.erp.module.academic.domain.Batch;
import com.college.erp.module.academic.domain.Term;
import com.college.erp.module.academic.domain.TermRepository;
import com.college.erp.module.academic.dto.CreateTermRequest;
import com.college.erp.module.academic.dto.TermDto;
import com.college.erp.tenant.TenantContext;

import lombok.RequiredArgsConstructor;

/** Not a dedicated file in the docs' file tree either — see AcademicYearService javadoc. */
@Service
@RequiredArgsConstructor
public class TermService {

    private static final String MODULE = "ACADEMIC";

    private final TermRepository termRepository;
    private final AcademicYearRepository academicYearRepository;
    private final AuditLogWriter auditLogWriter;

    @Transactional
    public TermDto create(CreateTermRequest request) {
        AcademicYear academicYear = academicYearRepository.findByIdAndDeletedFalse(request.getAcademicYearId())
                .orElseThrow(() -> new ResourceNotFoundException("AcademicYear", request.getAcademicYearId()));

        Term term = Term.builder()
                .academicYear(academicYear)
                .name(request.getName())
                .termNumber(request.getTermNumber())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .examStartDate(request.getExamStartDate())
                .examEndDate(request.getExamEndDate())
                .build();
        term.setTenantId(TenantContext.getCurrentTenantId());

        Term saved = termRepository.save(term);
        auditLogWriter.write(AuditAction.CREATE, MODULE, "Term", saved.getId().toString(),
                "Created: " + saved.getName());

        return TermDto.from(saved);
    }

    @Transactional(readOnly = true)
    public TermDto getById(UUID id) {
        return TermDto.from(getEntity(id));
    }

    @Transactional(readOnly = true)
    public List<TermDto> listByAcademicYear(UUID academicYearId) {
        return termRepository.findByAcademicYearIdAndDeletedFalse(academicYearId).stream()
                .map(TermDto::from)
                .toList();
    }

    /** For other modules that need the managed entity — see BatchService.getBatchEntity javadoc. */
    @Transactional(readOnly = true)
    public Term getTermEntity(UUID id) {
        return getEntity(id);
    }

    /**
     * "What term is this batch currently in" — a Batch only stores currentTermNumber + its
     * academic year, not a direct term_id, so this join lives here rather than being
     * re-implemented in module.student. Used when enrolling a student (initial enrollment is
     * created in the batch's current term).
     */
    @Transactional(readOnly = true)
    public Term getCurrentTermEntityForBatch(Batch batch) {
        return termRepository.findByAcademicYearIdAndTermNumberAndDeletedFalse(
                        batch.getAcademicYear().getId(), batch.getCurrentTermNumber())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Term", "number " + batch.getCurrentTermNumber() + " for batch " + batch.getLabel()));
    }

    /** module.finance resolves a term by (academic year, term number) rather than by id — that's
     *  what GenerateInvoiceRequest carries (05-api-documentation.md §7). */
    @Transactional(readOnly = true)
    public Term getTermByAcademicYearAndNumber(UUID academicYearId, int termNumber) {
        return termRepository.findByAcademicYearIdAndTermNumberAndDeletedFalse(academicYearId, termNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Term", "number " + termNumber + " in academic year " + academicYearId));
    }

    private Term getEntity(UUID id) {
        return termRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Term", id));
    }
}
