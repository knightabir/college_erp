package com.college.erp.module.asset.domain;

import com.college.erp.common.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 04-database-schema.md §12.1. */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "asset_categories")
public class AssetCategory extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** Whole-number percent, straight-line — see DepreciationService. */
    @Column(name = "depreciation_rate_pct")
    private Integer depreciationRatePct;
}
