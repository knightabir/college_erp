package com.college.erp.module.hr.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DesignationRepository extends JpaRepository<Designation, UUID> {

    Optional<Designation> findByIdAndDeletedFalse(UUID id);

    List<Designation> findByDeletedFalse();
}
