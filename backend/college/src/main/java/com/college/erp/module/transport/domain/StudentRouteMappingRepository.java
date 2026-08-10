package com.college.erp.module.transport.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRouteMappingRepository extends JpaRepository<StudentRouteMapping, UUID> {

    Optional<StudentRouteMapping> findByIdAndDeletedFalse(UUID id);

    List<StudentRouteMapping> findByRouteIdAndActiveTrueAndDeletedFalse(UUID routeId);

    Optional<StudentRouteMapping> findByStudentIdAndActiveTrueAndDeletedFalse(UUID studentId);

    boolean existsByStudentIdAndActiveTrueAndDeletedFalse(UUID studentId);
}
