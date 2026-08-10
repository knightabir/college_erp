package com.college.erp.module.student.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GuardianRepository extends JpaRepository<Guardian, UUID> {

    List<Guardian> findByStudentIdAndDeletedFalse(UUID studentId);

    Optional<Guardian> findByIdAndDeletedFalse(UUID id);
}
