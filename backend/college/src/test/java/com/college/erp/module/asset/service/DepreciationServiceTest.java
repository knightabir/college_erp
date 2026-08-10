package com.college.erp.module.asset.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.college.erp.common.exception.BusinessRuleViolationException;
import com.college.erp.module.asset.domain.Asset;
import com.college.erp.module.asset.domain.AssetCategory;
import com.college.erp.module.asset.dto.DepreciationDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * 03-srs.md [AST-005]. Named to match the module's other *ServiceTest files — see
 * DepreciationService javadoc for why this is an on-demand calculation, not a persisted schedule.
 */
@ExtendWith(MockitoExtension.class)
class DepreciationServiceTest {

    @Mock
    private AssetService assetService;

    private DepreciationService depreciationService;

    private Asset buildAsset(BigDecimal purchaseCost, LocalDate purchaseDate, Integer ratePct) {
        AssetCategory category = ratePct != null
                ? AssetCategory.builder().name("Computers").depreciationRatePct(ratePct).build()
                : null;
        Asset asset = Asset.builder()
                .assetTag("COM-000001").name("Laptop")
                .category(category).purchaseCost(purchaseCost).purchaseDate(purchaseDate)
                .build();
        asset.setId(UUID.randomUUID());
        return asset;
    }

    @Test
    @DisplayName("calculateCurrentValue() — straight-line: cost - (cost * rate% * yearsElapsed)")
    void calculateCurrentValue_straightLine() {
        // ARRANGE
        depreciationService = new DepreciationService(assetService);
        Asset asset = buildAsset(BigDecimal.valueOf(100_000), LocalDate.now().minusYears(3), 10);
        when(assetService.getAssetEntity(asset.getId())).thenReturn(asset);

        // ACT — 3 years * 10% * 100,000 = 30,000 depreciated, 70,000 remaining
        DepreciationDto dto = depreciationService.calculateCurrentValue(asset.getId());

        // ASSERT
        assertThat(dto.getYearsElapsed()).isEqualTo(3);
        assertThat(dto.getAccumulatedDepreciation()).isEqualByComparingTo("30000.00");
        assertThat(dto.getCurrentValue()).isEqualByComparingTo("70000.00");
    }

    @Test
    @DisplayName("calculateCurrentValue() — fully depreciated assets floor at zero, never negative")
    void calculateCurrentValue_floorsAtZero() {
        // ARRANGE
        depreciationService = new DepreciationService(assetService);
        Asset asset = buildAsset(BigDecimal.valueOf(50_000), LocalDate.now().minusYears(20), 10);
        when(assetService.getAssetEntity(asset.getId())).thenReturn(asset);

        // ACT — 20 years * 10% would be 200% of cost; capped at the full purchase cost
        DepreciationDto dto = depreciationService.calculateCurrentValue(asset.getId());

        // ASSERT
        assertThat(dto.getAccumulatedDepreciation()).isEqualByComparingTo("50000.00");
        assertThat(dto.getCurrentValue()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("calculateCurrentValue() — missing purchase cost/date raises BusinessRuleViolationException")
    void calculateCurrentValue_missingPurchaseInfo_throws() {
        // ARRANGE
        depreciationService = new DepreciationService(assetService);
        Asset asset = buildAsset(null, null, 10);
        when(assetService.getAssetEntity(asset.getId())).thenReturn(asset);

        // ACT / ASSERT
        assertThatThrownBy(() -> depreciationService.calculateCurrentValue(asset.getId()))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("calculateCurrentValue() — category with no depreciation_rate_pct raises BusinessRuleViolationException")
    void calculateCurrentValue_noCategoryRate_throws() {
        // ARRANGE
        depreciationService = new DepreciationService(assetService);
        Asset asset = buildAsset(BigDecimal.valueOf(50_000), LocalDate.now().minusYears(1), null);
        when(assetService.getAssetEntity(asset.getId())).thenReturn(asset);

        // ACT / ASSERT
        assertThatThrownBy(() -> depreciationService.calculateCurrentValue(asset.getId()))
                .isInstanceOf(BusinessRuleViolationException.class);
    }
}
