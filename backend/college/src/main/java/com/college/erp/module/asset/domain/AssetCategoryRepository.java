package com.college.erp.module.asset.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetCategoryRepository extends JpaRepository<AssetCategory, UUID> {

    Optional<AssetCategory> findByIdAndDeletedFalse(UUID id);

    List<AssetCategory> findByDeletedFalse();

    long countByDeletedFalse();
}
