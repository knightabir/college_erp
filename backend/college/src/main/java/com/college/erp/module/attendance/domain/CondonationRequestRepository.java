package com.college.erp.module.attendance.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CondonationRequestRepository extends JpaRepository<CondonationRequest, UUID> {

    Optional<CondonationRequest> findByIdAndDeletedFalse(UUID id);

    List<CondonationRequest> findByStudentIdAndDeletedFalse(UUID studentId);
}
