package com.college.erp.module.hr.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, UUID> {

    Optional<LeaveBalance> findByUserIdAndLeaveTypeIdAndYearAndDeletedFalse(
            UUID userId, UUID leaveTypeId, int year);

    List<LeaveBalance> findByUserIdAndYearAndDeletedFalse(UUID userId, int year);

    boolean existsByUserIdAndLeaveTypeIdAndYearAndDeletedFalse(UUID userId, UUID leaveTypeId, int year);
}
