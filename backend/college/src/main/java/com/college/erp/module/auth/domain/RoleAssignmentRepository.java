package com.college.erp.module.auth.domain;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoleAssignmentRepository extends JpaRepository<RoleAssignment, UUID> {

    List<RoleAssignment> findByUserId(UUID userId);

    @Query("""
        SELECT ra FROM RoleAssignment ra
        WHERE ra.userId = :userId
        AND ra.active = true
        AND (ra.expiresAt IS NULL OR ra.expiresAt > CURRENT_TIMESTAMP)
        """)
    List<RoleAssignment> findActiveByUserId(@Param("userId") UUID userId);

    /** Used by the startup bootstrap check (UserManagementService.bootstrapSuperAdminIfMissing)
     *  to decide whether a first account still needs creating — deliberately ignores expiry, since
     *  an expired-but-once-active SUPER_ADMIN grant still proves the system was bootstrapped. */
    boolean existsByRoleAndActiveTrue(UserRole role);
}
