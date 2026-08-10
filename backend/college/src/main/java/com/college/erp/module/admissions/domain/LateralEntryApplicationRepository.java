package com.college.erp.module.admissions.domain;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LateralEntryApplicationRepository extends JpaRepository<LateralEntryApplication, UUID> {

    Optional<LateralEntryApplication> findByApplicationIdAndDeletedFalse(UUID applicationId);
}
