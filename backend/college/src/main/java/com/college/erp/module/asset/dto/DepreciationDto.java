package com.college.erp.module.asset.dto;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

/**
 * 03-srs.md [AST-005]. Computed on demand by DepreciationService — there is no
 * depreciation_records (or similarly named) table anywhere in 04-database-schema.md to persist an
 * annual schedule into, so this reflects straight-line depreciation as of "now" rather than a
 * stored yearly snapshot. See DepreciationService javadoc.
 */
@Getter
@Builder
public class DepreciationDto {

    private UUID assetId;
    private BigDecimal purchaseCost;
    private Integer depreciationRatePct;
    private long yearsElapsed;
    private BigDecimal accumulatedDepreciation;
    private BigDecimal currentValue;
}
