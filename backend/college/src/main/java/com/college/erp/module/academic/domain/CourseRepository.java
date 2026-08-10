package com.college.erp.module.academic.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, UUID> {

    Page<Course> findByDeletedFalse(Pageable pageable);

    Optional<Course> findByIdAndDeletedFalse(UUID id);

    List<Course> findByDepartmentIdAndDeletedFalse(UUID departmentId);

    boolean existsByCodeAndDeletedFalse(String code);
}
