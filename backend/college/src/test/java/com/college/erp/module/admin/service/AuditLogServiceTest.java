package com.college.erp.module.admin.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLog;
import com.college.erp.audit.AuditLogRepository;
import com.college.erp.module.admin.dto.AuditLogDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 03-srs.md [AUDIT-004]. Named to match the module's other *ServiceTest files. */
@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    private AuditLogService auditLogService;

    @Test
    @DisplayName("search() — passes every filter through to the repository's search query")
    void search_passesFiltersThrough() {
        // ARRANGE
        auditLogService = new AuditLogService(auditLogRepository);
        AuditLog entry = AuditLog.builder().username("jdoe").action(AuditAction.FEE_COLLECTED)
                .module("FINANCE").entityType("FeePayment").entityId(UUID.randomUUID().toString())
                .description("Receipt RCP-2026-001042").build();
        UUID userId = UUID.randomUUID();
        Instant from = Instant.now().minusSeconds(3600);
        Instant to = Instant.now();
        var pageable = PageRequest.of(0, 50);
        when(auditLogRepository.search(eq(userId), eq("FeePayment"), eq(AuditAction.FEE_COLLECTED),
                eq(from), eq(to), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(entry)));

        // ACT
        var page = auditLogService.search(userId, "FeePayment", AuditAction.FEE_COLLECTED, from, to, pageable);

        // ASSERT
        assertThat(page.getContent()).hasSize(1);
        AuditLogDto dto = page.getContent().get(0);
        assertThat(dto.getUsername()).isEqualTo("jdoe");
        assertThat(dto.getAction()).isEqualTo(AuditAction.FEE_COLLECTED);
        verify(auditLogRepository).search(eq(userId), eq("FeePayment"), eq(AuditAction.FEE_COLLECTED),
                eq(from), eq(to), eq(pageable));
    }
}
