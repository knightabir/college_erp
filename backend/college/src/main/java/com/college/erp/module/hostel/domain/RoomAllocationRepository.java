package com.college.erp.module.hostel.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomAllocationRepository extends JpaRepository<RoomAllocation, UUID> {

    Optional<RoomAllocation> findByIdAndDeletedFalse(UUID id);

    List<RoomAllocation> findByRoomIdAndStatusAndDeletedFalse(UUID roomId, RoomAllocationStatus status);

    long countByRoomIdAndStatusAndDeletedFalse(UUID roomId, RoomAllocationStatus status);

    Optional<RoomAllocation> findByStudentIdAndStatusAndDeletedFalse(UUID studentId, RoomAllocationStatus status);
}
