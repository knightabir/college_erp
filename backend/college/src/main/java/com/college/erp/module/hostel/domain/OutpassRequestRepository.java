package com.college.erp.module.hostel.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OutpassRequestRepository extends JpaRepository<OutpassRequest, UUID> {

    Optional<OutpassRequest> findByIdAndDeletedFalse(UUID id);

    List<OutpassRequest> findByStudentIdAndDeletedFalseOrderByFromDatetimeDesc(UUID studentId);

    List<OutpassRequest> findByStatusAndDeletedFalse(OutpassStatus status);
}
