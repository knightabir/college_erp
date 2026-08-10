package com.college.erp.module.hr.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveTypeRepository extends JpaRepository<LeaveType, UUID> {

    Optional<LeaveType> findByIdAndDeletedFalse(UUID id);

    List<LeaveType> findByDeletedFalse();

    boolean existsByCodeAndDeletedFalse(String code);
}
