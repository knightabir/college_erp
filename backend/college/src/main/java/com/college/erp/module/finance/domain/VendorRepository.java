package com.college.erp.module.finance.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorRepository extends JpaRepository<Vendor, UUID> {

    Optional<Vendor> findByIdAndDeletedFalse(UUID id);

    List<Vendor> findByActiveTrueAndDeletedFalse();

    boolean existsByCodeAndDeletedFalse(String code);
}
