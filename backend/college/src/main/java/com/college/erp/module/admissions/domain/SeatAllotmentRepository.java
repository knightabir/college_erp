package com.college.erp.module.admissions.domain;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatAllotmentRepository extends JpaRepository<SeatAllotment, UUID> {

    Optional<SeatAllotment> findByIdAndDeletedFalse(UUID id);

    Optional<SeatAllotment> findByApplicationIdAndDeletedFalse(UUID applicationId);
}
