package com.college.erp.module.communication.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogWriter;
import com.college.erp.module.communication.domain.Notice;
import com.college.erp.module.communication.domain.NoticeRepository;
import com.college.erp.module.communication.dto.CreateNoticeRequest;
import com.college.erp.module.communication.dto.NoticeDto;

import lombok.RequiredArgsConstructor;

/**
 * 03-srs.md [COMM-001]. "Get all active notices relevant to the current user" (§11) doesn't
 * define what "relevant" resolves to for a given caller — there's no student's-current-batch or
 * staff's-department accessor exposed cross-module today, and building one is out of scope for
 * this module. getRelevant() instead takes the caller's department/batch as explicit optional
 * parameters (the controller can be extended to resolve and pass them once such an accessor
 * exists) — ALL and ROLE notices are always included regardless.
 */
@Service
@RequiredArgsConstructor
public class NoticeService {

    private static final String MODULE = "COMMUNICATION";

    private final NoticeRepository noticeRepository;
    private final AuditLogWriter auditLogWriter;

    /** [COMM-001]: "Create and publish a notice" — publishedAt is set immediately. */
    @Transactional
    public NoticeDto create(CreateNoticeRequest request) {
        Notice notice = Notice.builder()
                .title(request.getTitle())
                .body(request.getBody())
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .pinned(request.isPinned())
                .publishedAt(Instant.now())
                .expiresAt(request.getExpiresAt())
                .build();
        Notice saved = noticeRepository.save(notice);

        auditLogWriter.write(AuditAction.CREATE, MODULE, "Notice", saved.getId().toString(),
                "Published: " + saved.getTitle());

        return NoticeDto.from(saved);
    }

    /** See class javadoc for what "relevant" means here — departmentId/batchId are optional. */
    @Transactional(readOnly = true)
    public List<NoticeDto> getRelevant(UUID departmentId, UUID batchId) {
        return noticeRepository.findRelevant(Instant.now(), departmentId, batchId).stream()
                .map(NoticeDto::from)
                .toList();
    }
}
