package com.college.erp.module.academic.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, UUID> {

    Page<Department> findByDeletedFalse(Pageable pageable);

    List<Department> findByActiveTrueAndDeletedFalse();

    Optional<Department> findByIdAndDeletedFalse(UUID id);

    Optional<Department> findByCodeAndDeletedFalse(String code);

    boolean existsByCodeAndDeletedFalse(String code);
}
