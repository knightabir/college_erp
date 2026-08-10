package com.college.erp.module.admissions.domain;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MeritListRepository extends JpaRepository<MeritList, UUID> {

    Optional<MeritList> findByIdAndDeletedFalse(UUID id);
}
