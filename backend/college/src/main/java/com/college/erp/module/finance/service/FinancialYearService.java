package com.college.erp.module.finance.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.BusinessRuleViolationException;
import com.college.erp.common.exception.DuplicateResourceException;
import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.module.finance.domain.FinancialYear;
import com.college.erp.module.finance.domain.FinancialYearRepository;
import com.college.erp.module.finance.domain.FinancialYearStatus;
import com.college.erp.module.finance.dto.CreateFinancialYearRequest;
import com.college.erp.module.finance.dto.FinancialYearDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 03-srs.md [FIN-009], 05-api-documentation.md §7. Close is a two-call handshake matching the API
 * doc's own description exactly: "The first call sets status to CLOSING_PENDING. The second call
 * completes the close" — whichever two distinct users call it (both already gated to
 * HEAD_ACCOUNTANT-or-ADMIN by @PreAuthorize), the same user may not provide both calls.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinancialYearService {

    private static final String MODULE = "FINANCE";

    private final FinancialYearRepository financialYearRepository;
    private final AuditLogWriter auditLogWriter;

    @Transactional
    public FinancialYearDto create(CreateFinancialYearRequest request) {
        if (financialYearRepository.existsByStatusAndDeletedFalse(FinancialYearStatus.OPEN)) {
            throw new BusinessRuleViolationException(
                    "A financial year is already OPEN — only one may be open at a time");
        }

        FinancialYear year = FinancialYear.builder()
                .label(request.getLabel())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();

        FinancialYear saved;
        try {
            saved = financialYearRepository.save(year);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new DuplicateResourceException("Financial year '" + request.getLabel() + "' already exists");
        }

        auditLogWriter.write(AuditAction.CREATE, MODULE, "FinancialYear", saved.getId().toString(),
                "Created: " + saved.getLabel());

        return FinancialYearDto.from(saved);
    }

    @Transactional
    public FinancialYearDto initiateOrCompleteClose(UUID id, UUID callerUserId) {
        FinancialYear year = getEntity(id);

        if (year.getStatus() == FinancialYearStatus.CLOSED) {
            throw new BusinessRuleViolationException("Financial year is already closed");
        }

        if (year.getStatus() == FinancialYearStatus.OPEN) {
            year.setStatus(FinancialYearStatus.CLOSING_PENDING);
            year.setClosedByUserId(callerUserId);
            financialYearRepository.save(year);

            auditLogWriter.write(AuditAction.UPDATE, MODULE, "FinancialYear", id.toString(),
                    "Close initiated for " + year.getLabel());
            log.info("Financial year close initiated: [{}]", year.getLabel());
            return FinancialYearDto.from(year);
        }

        // CLOSING_PENDING — this is the second call.
        if (callerUserId.equals(year.getClosedByUserId())) {
            throw new BusinessRuleViolationException(
                    "The same user cannot provide both approvals for closing a financial year");
        }

        year.setStatus(FinancialYearStatus.CLOSED);
        year.setApprovedByUserId(callerUserId);
        FinancialYear saved = financialYearRepository.save(year);

        auditLogWriter.write(AuditAction.FINANCIAL_YEAR_CLOSED, MODULE, "FinancialYear", id.toString(),
                "Closed: " + saved.getLabel());
        log.info("Financial year closed: [{}]", saved.getLabel());

        return FinancialYearDto.from(saved);
    }

    @Transactional(readOnly = true)
    public FinancialYearDto getCurrent() {
        return FinancialYearDto.from(financialYearRepository.findByStatusAndDeletedFalse(FinancialYearStatus.OPEN)
                .orElseThrow(() -> new ResourceNotFoundException("No financial year is currently OPEN")));
    }

    /** For other modules that need the managed entity — see BatchService.getBatchEntity javadoc. */
    @Transactional(readOnly = true)
    public FinancialYear getCurrentEntity() {
        return financialYearRepository.findByStatusAndDeletedFalse(FinancialYearStatus.OPEN)
                .orElseThrow(() -> new ResourceNotFoundException("No financial year is currently OPEN"));
    }

    @Transactional(readOnly = true)
    public FinancialYear getEntity(UUID id) {
        return financialYearRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("FinancialYear", id));
    }
}
