package com.college.erp.module.asset.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** 05-api-documentation.md §9: POST /assets/{id}/transfer. */
@Getter
@Setter
public class TransferAssetRequest {

    @NotNull
    private UUID toDepartmentId;

    private UUID toUserId;

    private String toLocation;

    private String reason;
}
