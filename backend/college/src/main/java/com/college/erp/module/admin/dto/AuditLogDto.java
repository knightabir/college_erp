package com.college.erp.module.admin.dto;

import java.time.Instant;
import java.util.UUID;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLog;

import lombok.Builder;
import lombok.Getter;

/** 05-api-documentation.md §13's GET /admin/audit-log response shape. */
@Getter
@Builder
public class AuditLogDto {

    private UUID id;
    private String username;
    private AuditAction action;
    private String module;
    private String entityType;
    private String entityId;
    private String description;
    private Instant occurredAt;

    public static AuditLogDto from(AuditLog log) {
        return AuditLogDto.builder()
                .id(log.getId())
                .username(log.getUsername())
                .action(log.getAction())
                .module(log.getModule())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .description(log.getDescription())
                .occurredAt(log.getOccurredAt())
                .build();
    }
}
