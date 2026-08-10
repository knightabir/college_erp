package com.college.erp.module.placement.dto;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;

/** PlacementAnalyticsService's summary for an academic year — INVENTED, see V016's header comment. */
@Getter
@Builder
public class PlacementStatsDto {

    private long totalDriveApplications;
    private long totalOffers;
    private long acceptedOffers;
    private BigDecimal averageCtc;
    private BigDecimal highestCtc;
}
