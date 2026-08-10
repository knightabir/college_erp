package com.college.erp.module.finance.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, UUID> {

    Optional<PurchaseOrder> findByIdAndDeletedFalse(UUID id);

    List<PurchaseOrder> findByVendorIdAndDeletedFalse(UUID vendorId);
}
