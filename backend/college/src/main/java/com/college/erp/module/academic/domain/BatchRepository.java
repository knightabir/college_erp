package com.college.erp.module.academic.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BatchRepository extends JpaRepository<Batch, UUID> {

    Optional<Batch> findByIdAndDeletedFalse(UUID id);

    List<Batch> findByCourseIdAndDeletedFalse(UUID courseId);

    boolean existsByCourseIdAndAcademicYearIdAndDeletedFalse(UUID courseId, UUID academicYearId);
}
