package com.college.erp.module.asset.domain;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetCustodyLogRepository extends JpaRepository<AssetCustodyLog, UUID> {

    /** Oldest first — see 05-api-documentation.md §9's GET /assets/{id}/custody-chain. */
    List<AssetCustodyLog> findByAssetIdOrderByTransferredAtAsc(UUID assetId);
}
