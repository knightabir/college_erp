package com.college.erp.module.attendance.domain;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, UUID> {

    List<AttendanceRecord> findBySessionIdAndDeletedFalse(UUID sessionId);

    /** Physical delete, not soft — only DRAFT-session records are ever re-marked (03-srs.md
     *  [ATT-002]), and DRAFT records carry no audit/reporting weight yet; only audit_log,
     *  ledger_entries, and the two custody logs are truly append-only (04-database-schema.md §17). */
    void deleteBySessionId(UUID sessionId);

    @Query("""
        SELECT ar FROM AttendanceRecord ar
        WHERE ar.student.id = :studentId
        AND ar.session.offering.id = :offeringId
        AND ar.deleted = false
        """)
    List<AttendanceRecord> findByStudentAndOffering(
            @Param("studentId") UUID studentId, @Param("offeringId") UUID offeringId);

    @Query("""
        SELECT DISTINCT ar.student.id FROM AttendanceRecord ar
        WHERE ar.session.offering.id = :offeringId
        AND ar.deleted = false
        """)
    List<UUID> findDistinctStudentIdsByOffering(@Param("offeringId") UUID offeringId);
}
