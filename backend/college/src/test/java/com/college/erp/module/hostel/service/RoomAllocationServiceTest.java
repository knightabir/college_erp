package com.college.erp.module.hostel.service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.BusinessRuleViolationException;
import com.college.erp.module.hostel.domain.HostelBlock;
import com.college.erp.module.hostel.domain.HostelGender;
import com.college.erp.module.hostel.domain.Room;
import com.college.erp.module.hostel.domain.RoomAllocation;
import com.college.erp.module.hostel.domain.RoomAllocationRepository;
import com.college.erp.module.hostel.domain.RoomAllocationStatus;
import com.college.erp.module.hostel.dto.AllocateRoomRequest;
import com.college.erp.module.hostel.dto.RoomAllocationDto;
import com.college.erp.module.student.domain.Student;
import com.college.erp.module.student.service.StudentService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** 04-database-schema.md §14.1. Named to match the module's other *ServiceTest files. */
@ExtendWith(MockitoExtension.class)
class RoomAllocationServiceTest {

    @Mock
    private RoomAllocationRepository roomAllocationRepository;
    @Mock
    private RoomService roomService;
    @Mock
    private StudentService studentService;
    @Mock
    private AuditLogWriter auditLogWriter;

    private RoomAllocationService roomAllocationService;

    private Room buildRoom(int capacity) {
        HostelBlock block = HostelBlock.builder().name("Block A").gender(HostelGender.MIXED).build();
        block.setId(UUID.randomUUID());
        Room room = Room.builder().block(block).roomNumber("101").capacity(capacity).build();
        room.setId(UUID.randomUUID());
        return room;
    }

    private Student buildStudent() {
        Student student = Student.builder().enrollmentNumber("2024CSE001").firstName("Arjun")
                .lastName("Kumar").build();
        student.setId(UUID.randomUUID());
        return student;
    }

    @Test
    @DisplayName("allocate() — under capacity: succeeds")
    void allocate_underCapacity_succeeds() {
        // ARRANGE
        roomAllocationService = new RoomAllocationService(roomAllocationRepository, roomService, studentService,
                auditLogWriter);
        Room room = buildRoom(2);
        Student student = buildStudent();
        when(roomService.getRoomEntity(room.getId())).thenReturn(room);
        when(studentService.getStudentEntity(student.getId())).thenReturn(student);
        when(roomAllocationRepository.countByRoomIdAndStatusAndDeletedFalse(room.getId(), RoomAllocationStatus.ACTIVE))
                .thenReturn(1L);
        when(roomAllocationRepository.save(any(RoomAllocation.class))).thenAnswer(inv -> {
            RoomAllocation allocation = inv.getArgument(0);
            allocation.setId(UUID.randomUUID());
            return allocation;
        });

        AllocateRoomRequest request = new AllocateRoomRequest();
        request.setRoomId(room.getId());
        request.setStudentId(student.getId());
        request.setFromDate(LocalDate.now());

        // ACT
        RoomAllocationDto dto = roomAllocationService.allocate(request);

        // ASSERT
        assertThat(dto.getStatus()).isEqualTo(RoomAllocationStatus.ACTIVE);
        assertThat(dto.getRoomId()).isEqualTo(room.getId());
    }

    @Test
    @DisplayName("allocate() — at capacity: rejected")
    void allocate_atCapacity_throws() {
        // ARRANGE
        roomAllocationService = new RoomAllocationService(roomAllocationRepository, roomService, studentService,
                auditLogWriter);
        Room room = buildRoom(2);
        Student student = buildStudent();
        when(roomService.getRoomEntity(room.getId())).thenReturn(room);
        when(studentService.getStudentEntity(student.getId())).thenReturn(student);
        when(roomAllocationRepository.countByRoomIdAndStatusAndDeletedFalse(room.getId(), RoomAllocationStatus.ACTIVE))
                .thenReturn(2L);

        AllocateRoomRequest request = new AllocateRoomRequest();
        request.setRoomId(room.getId());
        request.setStudentId(student.getId());
        request.setFromDate(LocalDate.now());

        // ACT / ASSERT
        assertThatThrownBy(() -> roomAllocationService.allocate(request))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("vacate() — sets status VACATED and to_date")
    void vacate_setsStatusAndToDate() {
        // ARRANGE
        roomAllocationService = new RoomAllocationService(roomAllocationRepository, roomService, studentService,
                auditLogWriter);
        Room room = buildRoom(2);
        Student student = buildStudent();
        RoomAllocation allocation = RoomAllocation.builder().room(room).student(student)
                .fromDate(LocalDate.now().minusMonths(1)).status(RoomAllocationStatus.ACTIVE).build();
        UUID allocationId = UUID.randomUUID();
        allocation.setId(allocationId);
        when(roomAllocationRepository.findByIdAndDeletedFalse(allocationId)).thenReturn(Optional.of(allocation));
        when(roomAllocationRepository.save(any(RoomAllocation.class))).thenAnswer(inv -> inv.getArgument(0));

        // ACT
        RoomAllocationDto dto = roomAllocationService.vacate(allocationId);

        // ASSERT
        assertThat(dto.getStatus()).isEqualTo(RoomAllocationStatus.VACATED);
        assertThat(dto.getToDate()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("vacate() — already-vacated allocation cannot be vacated again")
    void vacate_alreadyVacated_throws() {
        // ARRANGE
        roomAllocationService = new RoomAllocationService(roomAllocationRepository, roomService, studentService,
                auditLogWriter);
        RoomAllocation allocation = RoomAllocation.builder().room(buildRoom(2)).student(buildStudent())
                .fromDate(LocalDate.now().minusMonths(2)).toDate(LocalDate.now().minusDays(1))
                .status(RoomAllocationStatus.VACATED).build();
        UUID allocationId = UUID.randomUUID();
        allocation.setId(allocationId);
        when(roomAllocationRepository.findByIdAndDeletedFalse(allocationId)).thenReturn(Optional.of(allocation));

        // ACT / ASSERT
        assertThatThrownBy(() -> roomAllocationService.vacate(allocationId))
                .isInstanceOf(BusinessRuleViolationException.class);
    }
}
