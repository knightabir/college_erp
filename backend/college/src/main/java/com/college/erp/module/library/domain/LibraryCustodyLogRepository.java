package com.college.erp.module.library.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LibraryCustodyLogRepository extends JpaRepository<LibraryCustodyLog, UUID> {

    @Query("""
        SELECT l FROM LibraryCustodyLog l
        WHERE l.copy.id = :copyId
        AND l.returnedAt IS NULL
        """)
    Optional<LibraryCustodyLog> findActiveByCopyId(@Param("copyId") UUID copyId);

    @Query("""
        SELECT l FROM LibraryCustodyLog l
        WHERE l.borrowerUserId = :borrowerUserId
        AND l.returnedAt IS NULL
        AND l.dueAt < CURRENT_TIMESTAMP
        """)
    List<LibraryCustodyLog> findOverdueByBorrower(@Param("borrowerUserId") UUID borrowerUserId);

    @Query("""
        SELECT COUNT(l) FROM LibraryCustodyLog l
        WHERE l.borrowerUserId = :borrowerUserId
        AND l.returnedAt IS NULL
        """)
    long countActiveLoans(@Param("borrowerUserId") UUID borrowerUserId);

    List<LibraryCustodyLog> findByCopyIdOrderByIssuedAtDesc(UUID copyId);

    /** LibraryOverdueScanJob: every overdue loan system-wide, not scoped to one borrower. */
    @Query("""
        SELECT l FROM LibraryCustodyLog l
        WHERE l.returnedAt IS NULL
        AND l.dueAt < CURRENT_TIMESTAMP
        """)
    List<LibraryCustodyLog> findAllOverdue();
}
