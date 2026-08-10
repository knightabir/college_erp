package com.college.erp.module.hr.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {

    Optional<LeaveRequest> findByIdAndDeletedFalse(UUID id);

    List<LeaveRequest> findByUserIdAndDeletedFalseOrderByFromDateDesc(UUID userId);

    List<LeaveRequest> findByStatusAndDeletedFalse(LeaveRequestStatus status);
}
