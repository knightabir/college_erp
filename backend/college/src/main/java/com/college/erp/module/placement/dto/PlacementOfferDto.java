package com.college.erp.module.placement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.college.erp.module.placement.domain.OfferStatus;
import com.college.erp.module.placement.domain.PlacementOffer;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlacementOfferDto {

    private UUID id;
    private UUID driveApplicationId;
    private BigDecimal ctc;
    private String designation;
    private LocalDate offerDate;
    private LocalDate joiningDate;
    private OfferStatus status;

    public static PlacementOfferDto from(PlacementOffer offer) {
        return PlacementOfferDto.builder()
                .id(offer.getId())
                .driveApplicationId(offer.getDriveApplication().getId())
                .ctc(offer.getCtc())
                .designation(offer.getDesignation())
                .offerDate(offer.getOfferDate())
                .joiningDate(offer.getJoiningDate())
                .status(offer.getStatus())
                .build();
    }
}
