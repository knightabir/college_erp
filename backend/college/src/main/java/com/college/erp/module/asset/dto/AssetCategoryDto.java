package com.college.erp.module.asset.dto;

import java.util.UUID;

import com.college.erp.module.asset.domain.AssetCategory;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AssetCategoryDto {

    private UUID id;
    private String name;
    private Integer depreciationRatePct;

    public static AssetCategoryDto from(AssetCategory category) {
        return AssetCategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .depreciationRatePct(category.getDepreciationRatePct())
                .build();
    }
}
