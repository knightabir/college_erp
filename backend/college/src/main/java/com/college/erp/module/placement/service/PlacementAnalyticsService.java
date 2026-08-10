package com.college.erp.module.placement.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.module.placement.domain.DriveApplication;
import com.college.erp.module.placement.domain.DriveApplicationRepository;
import com.college.erp.module.placement.domain.OfferStatus;
import com.college.erp.module.placement.domain.PlacementOffer;
import com.college.erp.module.placement.domain.PlacementOfferRepository;
import com.college.erp.module.placement.dto.PlacementStatsDto;

import lombok.RequiredArgsConstructor;

/**
 * INVENTED module — see V016__create_placement_tables.sql's header comment. Scoped per drive
 * (not per academic year) since that's the granularity cleanly queryable from what the schema
 * actually has — PlacementOffer has no direct academic_year_id, only via
 * driveApplication.drive.academicYear.
 */
@Service
@RequiredArgsConstructor
public class PlacementAnalyticsService {

    private final DriveApplicationRepository driveApplicationRepository;
    private final PlacementOfferRepository placementOfferRepository;

    @Transactional(readOnly = true)
    public PlacementStatsDto getStatsForDrive(UUID driveId) {
        List<DriveApplication> applications = driveApplicationRepository.findByDriveIdAndDeletedFalse(driveId);

        List<PlacementOffer> offers = applications.stream()
                .map(app -> placementOfferRepository.findByDriveApplicationIdAndDeletedFalse(app.getId()))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .toList();

        long accepted = offers.stream().filter(o -> o.getStatus() == OfferStatus.ACCEPTED).count();
        BigDecimal average = offers.isEmpty()
                ? BigDecimal.ZERO
                : offers.stream().map(PlacementOffer::getCtc).reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(offers.size()), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal highest = offers.stream().map(PlacementOffer::getCtc)
                .max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

        return PlacementStatsDto.builder()
                .totalDriveApplications(applications.size())
                .totalOffers(offers.size())
                .acceptedOffers(accepted)
                .averageCtc(average)
                .highestCtc(highest)
                .build();
    }
}
