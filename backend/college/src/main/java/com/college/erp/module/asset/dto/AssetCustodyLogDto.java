package com.college.erp.module.asset.dto;

import java.time.Instant;
import java.util.UUID;

import com.college.erp.module.asset.domain.AssetCustodyLog;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AssetCustodyLogDto {

    private UUID id;
    private UUID assetId;
    private UUID fromDepartmentId;
    private UUID toDepartmentId;
    private UUID fromUserId;
    private UUID assignedToUserId;
    private UUID transferredByUserId;
    private Instant transferredAt;
    private String reason;
    private String fromLocation;
    private String toLocation;

    public static AssetCustodyLogDto from(AssetCustodyLog log) {
        return AssetCustodyLogDto.builder()
                .id(log.getId())
                .assetId(log.getAsset().getId())
                .fromDepartmentId(log.getFromDepartmentId())
                .toDepartmentId(log.getToDepartmentId())
                .fromUserId(log.getFromUserId())
                .assignedToUserId(log.getAssignedToUserId())
                .transferredByUserId(log.getTransferredByUserId())
                .transferredAt(log.getTransferredAt())
                .reason(log.getReason())
                .fromLocation(log.getFromLocation())
                .toLocation(log.getToLocation())
                .build();
    }
}
