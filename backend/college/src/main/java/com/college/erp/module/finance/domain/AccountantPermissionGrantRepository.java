package com.college.erp.module.finance.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountantPermissionGrantRepository extends JpaRepository<AccountantPermissionGrant, UUID> {

    Optional<AccountantPermissionGrant> findByIdAndDeletedFalse(UUID id);

    List<AccountantPermissionGrant> findByGrantedByUserIdAndDeletedFalse(UUID grantedByUserId);

    @Query("""
        SELECT g FROM AccountantPermissionGrant g
        WHERE g.granteeUserId = :userId
        AND g.revokedAt IS NULL
        AND g.effectiveFrom <= CURRENT_TIMESTAMP
        AND (g.effectiveUntil IS NULL OR g.effectiveUntil > CURRENT_TIMESTAMP)
        AND g.deleted = false
        """)
    List<AccountantPermissionGrant> findActiveByGranteeUserId(@Param("userId") UUID userId);
}
