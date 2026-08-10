package com.college.erp.module.placement.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewRoundRepository extends JpaRepository<InterviewRound, UUID> {

    Optional<InterviewRound> findByIdAndDeletedFalse(UUID id);

    List<InterviewRound> findByDriveApplicationIdAndDeletedFalseOrderByRoundNumberAsc(UUID driveApplicationId);

    long countByDriveApplicationIdAndDeletedFalse(UUID driveApplicationId);
}
