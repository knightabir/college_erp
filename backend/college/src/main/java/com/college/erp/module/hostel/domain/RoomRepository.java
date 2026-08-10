package com.college.erp.module.hostel.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, UUID> {

    Optional<Room> findByIdAndDeletedFalse(UUID id);

    List<Room> findByBlockIdAndDeletedFalse(UUID blockId);

    boolean existsByBlockIdAndRoomNumberAndDeletedFalse(UUID blockId, String roomNumber);
}
