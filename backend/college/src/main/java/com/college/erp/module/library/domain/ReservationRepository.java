package com.college.erp.module.library.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    Optional<Reservation> findByIdAndDeletedFalse(UUID id);

    @Query("""
        SELECT r FROM Reservation r
        WHERE r.title.id = :titleId
        AND r.status IN (com.college.erp.module.library.domain.ReservationStatus.WAITING,
                         com.college.erp.module.library.domain.ReservationStatus.READY)
        AND r.deleted = false
        ORDER BY r.queuePosition ASC
        """)
    List<Reservation> findActiveByTitleOrderByQueuePosition(@Param("titleId") UUID titleId);

    @Query("""
        SELECT COUNT(r) > 0 FROM Reservation r
        WHERE r.title.id = :titleId
        AND r.status IN (com.college.erp.module.library.domain.ReservationStatus.WAITING,
                         com.college.erp.module.library.domain.ReservationStatus.READY)
        AND r.deleted = false
        """)
    boolean existsActiveByTitle(@Param("titleId") UUID titleId);

    long countByTitleIdAndStatusInAndDeletedFalse(UUID titleId, List<ReservationStatus> statuses);
}
