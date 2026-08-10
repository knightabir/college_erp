package com.college.erp.module.placement.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlacementDriveRepository extends JpaRepository<PlacementDrive, UUID> {

    Optional<PlacementDrive> findByIdAndDeletedFalse(UUID id);

    List<PlacementDrive> findByAcademicYearIdAndDeletedFalse(UUID academicYearId);

    List<PlacementDrive> findByStatusAndDeletedFalse(DriveStatus status);
}
