package com.college.erp.module.finance.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.DuplicateResourceException;
import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.module.finance.domain.FeeHead;
import com.college.erp.module.finance.domain.FeeHeadRepository;
import com.college.erp.module.finance.dto.CreateFeeHeadRequest;
import com.college.erp.module.finance.dto.FeeHeadDto;

import lombok.RequiredArgsConstructor;

/** Not a dedicated controller/service in the file tree — same justification as AccountHeadService. */
@Service
@RequiredArgsConstructor
public class FeeHeadService {

    private static final String MODULE = "FINANCE";

    private final FeeHeadRepository feeHeadRepository;
    private final AuditLogWriter auditLogWriter;

    @Transactional
    public FeeHeadDto create(CreateFeeHeadRequest request) {
        String code = request.getCode().toUpperCase();
        if (feeHeadRepository.findByCodeAndDeletedFalse(code).isPresent()) {
            throw new DuplicateResourceException("Fee head with code '" + code + "' already exists");
        }

        FeeHead feeHead = FeeHead.builder()
                .code(code)
                .name(request.getName())
                .description(request.getDescription())
                .build();

        FeeHead saved = feeHeadRepository.save(feeHead);
        auditLogWriter.write(AuditAction.CREATE, MODULE, "FeeHead", saved.getId().toString(),
                "Created: " + saved.getName());

        return FeeHeadDto.from(saved);
    }

    @Transactional(readOnly = true)
    public FeeHeadDto getById(UUID id) {
        return FeeHeadDto.from(getEntity(id));
    }

    /** For other modules that need the managed entity — see BatchService.getBatchEntity javadoc. */
    @Transactional(readOnly = true)
    public FeeHead getFeeHeadEntity(UUID id) {
        return getEntity(id);
    }

    @Transactional(readOnly = true)
    public List<FeeHeadDto> list() {
        return feeHeadRepository.findAll().stream()
                .filter(h -> !h.isDeleted())
                .map(FeeHeadDto::from)
                .toList();
    }

    private FeeHead getEntity(UUID id) {
        return feeHeadRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("FeeHead", id));
    }
}
