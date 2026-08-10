package com.college.erp.module.hr.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffProfileRepository extends JpaRepository<StaffProfile, UUID> {

    Optional<StaffProfile> findByIdAndDeletedFalse(UUID id);

    Optional<StaffProfile> findByUserIdAndDeletedFalse(UUID userId);

    List<StaffProfile> findByDepartmentIdAndDeletedFalse(UUID departmentId);

    List<StaffProfile> findByDeletedFalse();

    boolean existsByEmployeeNumber(String employeeNumber);

    boolean existsByUserIdAndDeletedFalse(UUID userId);

    long countByDeletedFalse();
}
