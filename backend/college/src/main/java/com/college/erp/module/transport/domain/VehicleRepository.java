package com.college.erp.module.transport.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

    Optional<Vehicle> findByIdAndDeletedFalse(UUID id);

    List<Vehicle> findByActiveTrueAndDeletedFalse();

    boolean existsByRegistrationNo(String registrationNo);
}
