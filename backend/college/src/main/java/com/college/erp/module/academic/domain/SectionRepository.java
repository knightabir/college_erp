package com.college.erp.module.academic.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SectionRepository extends JpaRepository<Section, UUID> {

    Optional<Section> findByIdAndDeletedFalse(UUID id);

    List<Section> findByBatchIdAndDeletedFalse(UUID batchId);

    boolean existsByBatchIdAndNameAndDeletedFalse(UUID batchId, String name);
}
