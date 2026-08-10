package com.college.erp.module.library.domain;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanPolicyRepository extends JpaRepository<LoanPolicy, UUID> {

    Optional<LoanPolicy> findByBorrowerTypeAndDeletedFalse(BorrowerType borrowerType);
}
