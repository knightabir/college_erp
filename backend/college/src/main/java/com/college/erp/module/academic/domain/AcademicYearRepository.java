package com.college.erp.module.academic.domain;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcademicYearRepository extends JpaRepository<AcademicYear, UUID> {

    Page<AcademicYear> findByDeletedFalse(Pageable pageable);

    Optional<AcademicYear> findByIdAndDeletedFalse(UUID id);

    Optional<AcademicYear> findByCurrentTrueAndDeletedFalse();

    boolean existsByLabelAndDeletedFalse(String label);
}
