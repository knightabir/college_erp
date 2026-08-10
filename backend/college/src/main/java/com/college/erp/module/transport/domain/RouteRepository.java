package com.college.erp.module.transport.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteRepository extends JpaRepository<Route, UUID> {

    Optional<Route> findByIdAndDeletedFalse(UUID id);

    List<Route> findByActiveTrueAndDeletedFalse();
}
