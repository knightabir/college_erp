package com.college.erp.module.asset.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.college.erp.module.asset.domain.Asset;
import com.college.erp.module.asset.domain.AssetStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AssetDto {

    private UUID id;
    private String assetTag;
    private String name;
    private String description;
    private UUID categoryId;
    private String categoryName;
    private UUID departmentId;
    private String departmentName;
    private String roomLocation;
    private LocalDate purchaseDate;
    private BigDecimal purchaseCost;
    private UUID vendorId;
    private LocalDate warrantyExpiry;
    private UUID assignedToUserId;
    private AssetStatus status;

    public static AssetDto from(Asset asset) {
        return AssetDto.builder()
                .id(asset.getId())
                .assetTag(asset.getAssetTag())
                .name(asset.getName())
                .description(asset.getDescription())
                .categoryId(asset.getCategory() != null ? asset.getCategory().getId() : null)
                .categoryName(asset.getCategory() != null ? asset.getCategory().getName() : null)
                .departmentId(asset.getDepartment().getId())
                .departmentName(asset.getDepartment().getName())
                .roomLocation(asset.getRoomLocation())
                .purchaseDate(asset.getPurchaseDate())
                .purchaseCost(asset.getPurchaseCost())
                .vendorId(asset.getVendor() != null ? asset.getVendor().getId() : null)
                .warrantyExpiry(asset.getWarrantyExpiry())
                .assignedToUserId(asset.getAssignedToUserId())
                .status(asset.getStatus())
                .build();
    }
}
