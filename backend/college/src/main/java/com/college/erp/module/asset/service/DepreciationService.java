package com.college.erp.module.asset.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.common.exception.BusinessRuleViolationException;
import com.college.erp.module.asset.domain.Asset;
import com.college.erp.module.asset.dto.DepreciationDto;

import lombok.RequiredArgsConstructor;

/**
 * 03-srs.md [AST-005]: "straight-line depreciation schedule based on the category's
 * depreciation_rate_pct and purchase_cost, recorded annually." There is no depreciation_records
 * (or similarly named) table anywhere in 04-database-schema.md, so nothing is persisted — this is
 * a pure on-demand calculation of current book value as of today, not a stored annual schedule.
 * Revisit (add a persisted per-year snapshot) if the schema ever grows such a table.
 */
@Service
@RequiredArgsConstructor
public class DepreciationService {

    private final AssetService assetService;

    @Transactional(readOnly = true)
    public DepreciationDto calculateCurrentValue(UUID assetId) {
        Asset asset = assetService.getAssetEntity(assetId);
        if (asset.getPurchaseCost() == null || asset.getPurchaseDate() == null) {
            throw new BusinessRuleViolationException(
                    "Asset has no purchase cost/date on record — depreciation cannot be calculated");
        }
        Integer ratePct = asset.getCategory() != null ? asset.getCategory().getDepreciationRatePct() : null;
        if (ratePct == null) {
            throw new BusinessRuleViolationException(
                    "Asset's category has no depreciation_rate_pct on record");
        }

        long yearsElapsed = Math.max(0, ChronoUnit.YEARS.between(asset.getPurchaseDate(), LocalDate.now()));
        BigDecimal annualDepreciation = asset.getPurchaseCost()
                .multiply(BigDecimal.valueOf(ratePct))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal accumulated = annualDepreciation.multiply(BigDecimal.valueOf(yearsElapsed));
        if (accumulated.compareTo(asset.getPurchaseCost()) > 0) {
            accumulated = asset.getPurchaseCost();
        }
        BigDecimal currentValue = asset.getPurchaseCost().subtract(accumulated);

        return DepreciationDto.builder()
                .assetId(asset.getId())
                .purchaseCost(asset.getPurchaseCost())
                .depreciationRatePct(ratePct)
                .yearsElapsed(yearsElapsed)
                .accumulatedDepreciation(accumulated)
                .currentValue(currentValue)
                .build();
    }
}
