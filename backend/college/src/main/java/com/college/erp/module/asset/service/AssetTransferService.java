package com.college.erp.module.asset.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.AccessDeniedException;
import com.college.erp.config.ErpProperties;
import com.college.erp.module.academic.domain.Department;
import com.college.erp.module.academic.service.DepartmentService;
import com.college.erp.module.asset.domain.Asset;
import com.college.erp.module.asset.domain.AssetCustodyLog;
import com.college.erp.module.asset.domain.AssetCustodyLogRepository;
import com.college.erp.module.asset.domain.AssetRepository;
import com.college.erp.module.asset.domain.AssetStatus;
import com.college.erp.module.asset.dto.AssetCustodyLogDto;
import com.college.erp.module.asset.dto.AssetDto;
import com.college.erp.module.asset.dto.DisposeAssetRequest;
import com.college.erp.module.asset.dto.TransferAssetRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 03-srs.md [AST-002] (custody log, append-only) and [AST-004] (disposal). transfer() writes an
 * immutable AssetCustodyLog row and moves the asset's current department/location/custodian —
 * exactly the shape 05-api-documentation.md §9's POST /assets/{id}/transfer returns.
 *
 * dispose(): [AST-004] reads "Asset disposal requires: Asset Manager to initiate + Admin to
 * confirm (if above value threshold)" — a two-party workflow. But 04-database-schema.md §12.2's
 * status enum (ACTIVE | UNDER_MAINTENANCE | DISPOSED | LOST | TRANSFERRED) has no intermediate
 * "pending disposal" value and there's no separate disposal-request table to hold an in-flight
 * initiation between two calls, so a literal two-step handshake can't be persisted without
 * inventing schema. Simplified to a single call with a role-based gate: below the configured
 * threshold (erp.asset.disposal-value-threshold) an ASSET_MANAGER may dispose directly (matching
 * 05-api-documentation.md §9's controller-level auth of SUPER_ADMIN/ASSET_MANAGER); at or above it
 * the caller must specifically hold SUPER_ADMIN — modeling "Admin to confirm" as "only the higher
 * of the two allowed roles may act, once the amount crosses the threshold." Revisit if a
 * disposal-request table is ever added to the schema.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetTransferService {

    private static final String MODULE = "ASSET";

    private final AssetRepository assetRepository;
    private final AssetCustodyLogRepository assetCustodyLogRepository;
    private final AssetService assetService;
    private final DepartmentService departmentService;
    private final ErpProperties erpProperties;
    private final AuditLogWriter auditLogWriter;

    @Transactional
    public AssetCustodyLogDto transfer(UUID assetId, TransferAssetRequest request, UUID transferredByUserId) {
        Asset asset = assetService.getAssetEntity(assetId);
        Department toDepartment = departmentService.getDepartmentEntity(request.getToDepartmentId());

        UUID fromDepartmentId = asset.getDepartment().getId();
        UUID fromUserId = asset.getAssignedToUserId();
        String fromLocation = asset.getRoomLocation();

        AssetCustodyLog custodyLog = AssetCustodyLog.builder()
                .asset(asset)
                .fromDepartmentId(fromDepartmentId)
                .toDepartmentId(toDepartment.getId())
                .fromUserId(fromUserId)
                .assignedToUserId(request.getToUserId())
                .transferredByUserId(transferredByUserId)
                .transferredAt(Instant.now())
                .reason(request.getReason())
                .fromLocation(fromLocation)
                .toLocation(request.getToLocation())
                .build();
        AssetCustodyLog saved = assetCustodyLogRepository.save(custodyLog);

        asset.setDepartment(toDepartment);
        asset.setAssignedToUserId(request.getToUserId());
        asset.setRoomLocation(request.getToLocation());
        assetRepository.save(asset);

        auditLogWriter.write(AuditAction.ASSET_TRANSFERRED, MODULE, "Asset", asset.getId().toString(),
                "Transferred " + asset.getAssetTag() + " to department " + toDepartment.getCode());
        log.info("Asset transferred: [{}] to department [{}]", asset.getAssetTag(), toDepartment.getCode());

        return AssetCustodyLogDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<AssetCustodyLogDto> custodyChain(UUID assetId) {
        return assetCustodyLogRepository.findByAssetIdOrderByTransferredAtAsc(assetId).stream()
                .map(AssetCustodyLogDto::from)
                .toList();
    }

    @Transactional
    public AssetDto dispose(UUID assetId, DisposeAssetRequest request) {
        Asset asset = assetService.getAssetEntity(assetId);
        assertDisposalAuthority(asset);

        asset.setStatus(AssetStatus.DISPOSED);
        Asset saved = assetRepository.save(asset);

        auditLogWriter.write(AuditAction.ASSET_DISPOSED, MODULE, "Asset", asset.getId().toString(),
                "Disposed " + asset.getAssetTag() + ": " + request.getReason());
        log.info("Asset disposed: [{}]", asset.getAssetTag());

        return AssetDto.from(saved);
    }

    /** See class javadoc — the role-gate stand-in for [AST-004]'s two-party approval. */
    private void assertDisposalAuthority(Asset asset) {
        boolean aboveThreshold = asset.getPurchaseCost() != null
                && asset.getPurchaseCost().compareTo(erpProperties.getAsset().getDisposalValueThreshold()) >= 0;
        if (!aboveThreshold) {
            return;
        }
        boolean isSuperAdmin = SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch(a -> a.equals("ROLE_SUPER_ADMIN"));
        if (!isSuperAdmin) {
            throw new AccessDeniedException(
                    "Disposing an asset valued at or above " + erpProperties.getAsset().getDisposalValueThreshold()
                            + " requires a Super Admin to confirm");
        }
    }
}
