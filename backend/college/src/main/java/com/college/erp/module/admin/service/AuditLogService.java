package com.college.erp.module.admin.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogRepository;
import com.college.erp.module.admin.dto.AuditLogDto;

import lombok.RequiredArgsConstructor;

/** 03-srs.md [AUDIT-004], 05-api-documentation.md §13 "Audit Log". */
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public Page<AuditLogDto> search(UUID userId, String entityType, AuditAction action, Instant from, Instant to,
            Pageable pageable) {
        return auditLogRepository.search(userId, entityType, action, from, to, pageable)
                .map(AuditLogDto::from);
    }
}
