package com.college.erp.module.finance.domain;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FeeHeadRepository extends JpaRepository<FeeHead, UUID> {

    Optional<FeeHead> findByIdAndDeletedFalse(UUID id);

    Optional<FeeHead> findByCodeAndDeletedFalse(String code);
}
