package com.college.erp.module.auth.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PermissionGrantRepository extends JpaRepository<PermissionGrant, UUID> {

    List<PermissionGrant> findByGranteeUserId(UUID granteeUserId);

    @Query("""
        SELECT pg FROM PermissionGrant pg
        WHERE pg.granteeUserId = :userId
        AND pg.revokedAt IS NULL
        AND pg.effectiveFrom <= CURRENT_TIMESTAMP
        AND (pg.effectiveUntil IS NULL OR pg.effectiveUntil > CURRENT_TIMESTAMP)
        """)
    List<PermissionGrant> findActiveByGranteeUserId(@Param("userId") UUID userId);

    /** PermissionGrantExpiryJob: active grants whose effective_until falls in [from, to). */
    @Query("""
        SELECT pg FROM PermissionGrant pg
        WHERE pg.revokedAt IS NULL
        AND pg.effectiveUntil IS NOT NULL
        AND pg.effectiveUntil >= :from
        AND pg.effectiveUntil < :to
        """)
    List<PermissionGrant> findExpiringBetween(@Param("from") Instant from, @Param("to") Instant to);
}
