package com.college.erp.module.finance.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.DuplicateResourceException;
import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.module.finance.domain.AccountHead;
import com.college.erp.module.finance.domain.AccountHeadRepository;
import com.college.erp.module.finance.domain.AccountType;
import com.college.erp.module.finance.dto.AccountHeadDto;
import com.college.erp.module.finance.dto.CreateAccountHeadRequest;

import lombok.RequiredArgsConstructor;

/**
 * Not a dedicated controller/service in 08-system-architecture.md's file tree — same situation as
 * module.academic's AcademicYearService/TermService: account_heads is a table other finance
 * writes (ledger entries, fee structures indirectly) depend on existing, so something has to be
 * able to create them. Also backs LedgerService's lazily-created system account heads (receivable/
 * fee income) via getOrCreateSystemHead.
 */
@Service
@RequiredArgsConstructor
public class AccountHeadService {

    private static final String MODULE = "FINANCE";

    private final AccountHeadRepository accountHeadRepository;
    private final AuditLogWriter auditLogWriter;

    @Transactional
    public AccountHeadDto create(CreateAccountHeadRequest request) {
        String code = request.getCode().toUpperCase();
        if (accountHeadRepository.findByCodeAndDeletedFalse(code).isPresent()) {
            throw new DuplicateResourceException("Account head with code '" + code + "' already exists");
        }

        AccountHead.AccountHeadBuilder builder = AccountHead.builder()
                .code(code)
                .name(request.getName())
                .description(request.getDescription())
                .type(request.getType());
        if (request.getParentId() != null) {
            builder.parent(getEntity(request.getParentId()));
        }

        AccountHead saved = accountHeadRepository.save(builder.build());
        auditLogWriter.write(AuditAction.CREATE, MODULE, "AccountHead", saved.getId().toString(),
                "Created: " + saved.getName());

        return AccountHeadDto.from(saved);
    }

    /** Idempotent — used by LedgerService so fee-collection double-entry posting never fails just
     *  because the two well-known system account heads haven't been manually seeded yet. */
    @Transactional
    public AccountHead getOrCreateSystemHead(String code, String name, AccountType type) {
        return accountHeadRepository.findByCodeAndDeletedFalse(code)
                .orElseGet(() -> accountHeadRepository.save(
                        AccountHead.builder().code(code).name(name).type(type).build()));
    }

    @Transactional(readOnly = true)
    public AccountHeadDto getById(UUID id) {
        return AccountHeadDto.from(getEntity(id));
    }

    /** For other modules that need the managed entity — see BatchService.getBatchEntity javadoc. */
    @Transactional(readOnly = true)
    public AccountHead getAccountHeadEntity(UUID id) {
        return getEntity(id);
    }

    @Transactional(readOnly = true)
    public List<AccountHeadDto> list() {
        return accountHeadRepository.findAll().stream()
                .filter(h -> !h.isDeleted())
                .map(AccountHeadDto::from)
                .toList();
    }

    private AccountHead getEntity(UUID id) {
        return accountHeadRepository.findById(id)
                .filter(h -> !h.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("AccountHead", id));
    }
}
