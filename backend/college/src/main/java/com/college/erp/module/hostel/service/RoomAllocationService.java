package com.college.erp.module.hostel.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.BusinessRuleViolationException;
import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.module.hostel.domain.Room;
import com.college.erp.module.hostel.domain.RoomAllocation;
import com.college.erp.module.hostel.domain.RoomAllocationRepository;
import com.college.erp.module.hostel.domain.RoomAllocationStatus;
import com.college.erp.module.hostel.dto.AllocateRoomRequest;
import com.college.erp.module.hostel.dto.RoomAllocationDto;
import com.college.erp.module.student.domain.Student;
import com.college.erp.module.student.service.StudentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 04-database-schema.md §14.1 — see the module's CLAUDE.md deviations note (no SRS/API basis).
 * The one business rule enforced here (room capacity) is ordinary domain sense, not a cited
 * requirement — there's no [HOSTEL-xxx] code to point to.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoomAllocationService {

    private static final String MODULE = "HOSTEL";

    private final RoomAllocationRepository roomAllocationRepository;
    private final RoomService roomService;
    private final StudentService studentService;
    private final AuditLogWriter auditLogWriter;

    @Transactional
    public RoomAllocationDto allocate(AllocateRoomRequest request) {
        Room room = roomService.getRoomEntity(request.getRoomId());
        Student student = studentService.getStudentEntity(request.getStudentId());

        long activeCount = roomAllocationRepository.countByRoomIdAndStatusAndDeletedFalse(
                room.getId(), RoomAllocationStatus.ACTIVE);
        if (activeCount >= room.getCapacity()) {
            throw new BusinessRuleViolationException(
                    "Room " + room.getRoomNumber() + " is at capacity (" + room.getCapacity() + ")");
        }

        RoomAllocation allocation = RoomAllocation.builder()
                .room(room)
                .student(student)
                .fromDate(request.getFromDate())
                .status(RoomAllocationStatus.ACTIVE)
                .build();
        RoomAllocation saved = roomAllocationRepository.save(allocation);

        auditLogWriter.write(AuditAction.CREATE, MODULE, "RoomAllocation", saved.getId().toString(),
                "Allocated " + student.getFullName() + " to room " + room.getRoomNumber());
        log.info("Room allocated: [{}] to student [{}]", room.getRoomNumber(), student.getEnrollmentNumber());

        return RoomAllocationDto.from(saved);
    }

    @Transactional
    public RoomAllocationDto vacate(UUID allocationId) {
        RoomAllocation allocation = getEntity(allocationId);
        if (allocation.getStatus() != RoomAllocationStatus.ACTIVE) {
            throw new BusinessRuleViolationException("Allocation is not active");
        }

        allocation.vacate(LocalDate.now());
        RoomAllocation saved = roomAllocationRepository.save(allocation);

        auditLogWriter.write(AuditAction.UPDATE, MODULE, "RoomAllocation", allocationId.toString(),
                "Vacated room " + allocation.getRoom().getRoomNumber());

        return RoomAllocationDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<RoomAllocationDto> listActiveByRoom(UUID roomId) {
        return roomAllocationRepository.findByRoomIdAndStatusAndDeletedFalse(roomId, RoomAllocationStatus.ACTIVE)
                .stream()
                .map(RoomAllocationDto::from)
                .toList();
    }

    private RoomAllocation getEntity(UUID id) {
        return roomAllocationRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("RoomAllocation", id));
    }
}
