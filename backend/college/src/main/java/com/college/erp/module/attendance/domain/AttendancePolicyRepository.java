package com.college.erp.module.attendance.domain;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendancePolicyRepository extends JpaRepository<AttendancePolicy, UUID> {

    Optional<AttendancePolicy> findByCourseIdAndDeletedFalse(UUID courseId);
}
