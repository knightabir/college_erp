package com.college.erp.module.asset.domain;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetRepository extends JpaRepository<Asset, UUID> {

    Optional<Asset> findByIdAndDeletedFalse(UUID id);

    Optional<Asset> findByAssetTagAndDeletedFalse(String assetTag);

    Page<Asset> findByDepartmentIdAndDeletedFalse(UUID departmentId, Pageable pageable);

    boolean existsByAssetTag(String assetTag);

    long countByDeletedFalse();
}
