package com.college.erp.module.placement.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DriveApplicationRepository extends JpaRepository<DriveApplication, UUID> {

    Optional<DriveApplication> findByIdAndDeletedFalse(UUID id);

    List<DriveApplication> findByDriveIdAndDeletedFalse(UUID driveId);

    List<DriveApplication> findByStudentIdAndDeletedFalse(UUID studentId);

    boolean existsByDriveIdAndStudentIdAndDeletedFalse(UUID driveId, UUID studentId);

    long countByDriveIdAndStatusAndDeletedFalse(UUID driveId, DriveApplicationStatus status);
}
