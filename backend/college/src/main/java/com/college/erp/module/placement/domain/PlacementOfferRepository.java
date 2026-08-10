package com.college.erp.module.placement.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlacementOfferRepository extends JpaRepository<PlacementOffer, UUID> {

    Optional<PlacementOffer> findByIdAndDeletedFalse(UUID id);

    Optional<PlacementOffer> findByDriveApplicationIdAndDeletedFalse(UUID driveApplicationId);

    List<PlacementOffer> findByStatusAndDeletedFalse(OfferStatus status);
}
