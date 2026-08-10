package com.college.erp.module.auth.domain;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByIdAndDeletedFalse(UUID id);

    Optional<User> findByUsernameAndDeletedFalse(String username);

    Optional<User> findByEmailAndDeletedFalse(String email);

    // Login accepts either identifier — derived query naming can't express the
    // "(username = x OR email = x) AND deleted = false" grouping, so this needs JPQL.
    @Query("""
        SELECT u FROM User u
        WHERE (u.username = :identifier OR u.email = :identifier)
        AND u.deleted = false
        """)
    Optional<User> findByUsernameOrEmail(@Param("identifier") String identifier);

    boolean existsByUsernameAndDeletedFalse(String username);

    boolean existsByEmailAndDeletedFalse(String email);

    Page<User> findByDeletedFalse(Pageable pageable);

    /** Tenant-scoped listing for UserManagementService.list() — see its class javadoc for why
     *  users/role_assignments are scoped in application code rather than Postgres RLS. */
    Page<User> findByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);
}
