package com.college.erp.module.finance.domain;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountHeadRepository extends JpaRepository<AccountHead, UUID> {

    Optional<AccountHead> findByCodeAndDeletedFalse(String code);
}
