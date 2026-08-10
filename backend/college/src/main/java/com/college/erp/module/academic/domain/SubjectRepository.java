package com.college.erp.module.academic.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectRepository extends JpaRepository<Subject, UUID> {

    Page<Subject> findByDeletedFalse(Pageable pageable);

    Optional<Subject> findByIdAndDeletedFalse(UUID id);

    List<Subject> findByDepartmentIdAndDeletedFalse(UUID departmentId);

    boolean existsByCodeAndDeletedFalse(String code);
}
