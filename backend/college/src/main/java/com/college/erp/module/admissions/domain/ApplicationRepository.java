package com.college.erp.module.admissions.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    Optional<Application> findByIdAndDeletedFalse(UUID id);

    Page<Application> findByCourseIdAndAcademicYearIdAndDeletedFalse(
            UUID courseId, UUID academicYearId, Pageable pageable);

    Page<Application> findByStatusAndDeletedFalse(ApplicationStatus status, Pageable pageable);

    List<Application> findByCourseIdAndAcademicYearIdAndStatusAndDeletedFalse(
            UUID courseId, UUID academicYearId, ApplicationStatus status);

    boolean existsByApplicationNumber(String applicationNumber);

    long countByDeletedFalse();
}
