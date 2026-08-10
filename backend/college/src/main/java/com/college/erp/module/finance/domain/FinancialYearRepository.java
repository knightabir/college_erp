package com.college.erp.module.finance.domain;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialYearRepository extends JpaRepository<FinancialYear, UUID> {

    Optional<FinancialYear> findByIdAndDeletedFalse(UUID id);

    Optional<FinancialYear> findByStatusAndDeletedFalse(FinancialYearStatus status);

    boolean existsByStatusAndDeletedFalse(FinancialYearStatus status);
}
