package com.college.erp.module.hostel.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HostelBlockRepository extends JpaRepository<HostelBlock, UUID> {

    Optional<HostelBlock> findByIdAndDeletedFalse(UUID id);

    List<HostelBlock> findByDeletedFalse();
}
