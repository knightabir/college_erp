package com.college.erp.module.finance.domain;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    @Query("""
        SELECT le FROM LedgerEntry le
        WHERE (:from IS NULL OR le.entryDate >= :from)
        AND (:to IS NULL OR le.entryDate <= :to)
        ORDER BY le.entryDate DESC, le.createdAt DESC
        """)
    Page<LedgerEntry> browse(@Param("from") LocalDate from, @Param("to") LocalDate to, Pageable pageable);
}
