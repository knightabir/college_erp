package com.college.erp.audit;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Package-private to this kernel package by convention (not enforced at the Java level since
 * audit is kernel infra every module is allowed to write through AuditLogWriter — but nothing
 * outside audit/ should query this repository directly; use AuditLogWriter for writes and the
 * admin module's AuditLogController for reads).
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findByEntityTypeAndEntityId(String entityType, String entityId, Pageable pageable);

    Page<AuditLog> findByUserId(UUID userId, Pageable pageable);

    Page<AuditLog> findByActionAndOccurredAtBetween(AuditAction action, Instant from, Instant to, Pageable pageable);

    /** [AUDIT-004]: "searchable by user, entity type, action, and date range" — all optional and
     *  combinable, which no derived-query method name can express. Each `:param IS NULL OR ...`
     *  clause makes that filter a no-op when the caller omits it. */
    @Query("""
        SELECT a FROM AuditLog a
        WHERE (:userId IS NULL OR a.userId = :userId)
        AND (:entityType IS NULL OR a.entityType = :entityType)
        AND (:action IS NULL OR a.action = :action)
        AND (:from IS NULL OR a.occurredAt >= :from)
        AND (:to IS NULL OR a.occurredAt <= :to)
        ORDER BY a.occurredAt DESC
        """)
    Page<AuditLog> search(@Param("userId") UUID userId, @Param("entityType") String entityType,
            @Param("action") AuditAction action, @Param("from") Instant from, @Param("to") Instant to,
            Pageable pageable);
}
