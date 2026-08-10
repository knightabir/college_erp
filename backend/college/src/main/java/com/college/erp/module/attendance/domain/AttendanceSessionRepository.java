package com.college.erp.module.attendance.domain;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, UUID> {

    Optional<AttendanceSession> findByIdAndDeletedFalse(UUID id);

    Optional<AttendanceSession> findByOfferingIdAndSectionIdAndSessionDateAndDeletedFalse(
            UUID offeringId, UUID sectionId, LocalDate sessionDate);

    boolean existsByOfferingIdAndSectionIdAndSessionDateAndDeletedFalse(
            UUID offeringId, UUID sectionId, LocalDate sessionDate);
}
