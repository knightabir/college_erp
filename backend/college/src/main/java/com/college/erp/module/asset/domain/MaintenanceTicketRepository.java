package com.college.erp.module.asset.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceTicketRepository extends JpaRepository<MaintenanceTicket, UUID> {

    Optional<MaintenanceTicket> findByIdAndDeletedFalse(UUID id);

    List<MaintenanceTicket> findByAssetIdAndDeletedFalse(UUID assetId);

    List<MaintenanceTicket> findByStatusAndDeletedFalse(TicketStatus status);
}
