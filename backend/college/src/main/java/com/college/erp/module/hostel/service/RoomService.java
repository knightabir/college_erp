package com.college.erp.module.hostel.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.DuplicateResourceException;
import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.module.hostel.domain.HostelBlock;
import com.college.erp.module.hostel.domain.Room;
import com.college.erp.module.hostel.domain.RoomRepository;
import com.college.erp.module.hostel.dto.CreateRoomRequest;
import com.college.erp.module.hostel.dto.RoomDto;

import lombok.RequiredArgsConstructor;

/** 04-database-schema.md §14.1 — see the module's CLAUDE.md deviations note (no SRS/API basis). */
@Service
@RequiredArgsConstructor
public class RoomService {

    private static final String MODULE = "HOSTEL";

    private final RoomRepository roomRepository;
    private final HostelBlockService hostelBlockService;
    private final AuditLogWriter auditLogWriter;

    @Transactional
    public RoomDto create(UUID blockId, CreateRoomRequest request) {
        HostelBlock block = hostelBlockService.getBlockEntity(blockId);
        if (roomRepository.existsByBlockIdAndRoomNumberAndDeletedFalse(blockId, request.getRoomNumber())) {
            throw new DuplicateResourceException(
                    "Room '" + request.getRoomNumber() + "' already exists in this block");
        }

        Room room = Room.builder()
                .block(block)
                .roomNumber(request.getRoomNumber())
                .floor(request.getFloor())
                .capacity(request.getCapacity())
                .roomType(request.getRoomType())
                .build();
        Room saved = roomRepository.save(room);

        auditLogWriter.write(AuditAction.CREATE, MODULE, "Room", saved.getId().toString(),
                "Added room " + saved.getRoomNumber() + " to " + block.getName());

        return RoomDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<RoomDto> listByBlock(UUID blockId) {
        return roomRepository.findByBlockIdAndDeletedFalse(blockId).stream()
                .map(RoomDto::from)
                .toList();
    }

    /** For RoomAllocationService — see BatchService.getBatchEntity javadoc. */
    @Transactional(readOnly = true)
    public Room getRoomEntity(UUID id) {
        return roomRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room", id));
    }
}
