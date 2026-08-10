package com.college.erp.module.transport.service;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.BusinessRuleViolationException;
import com.college.erp.module.student.domain.Student;
import com.college.erp.module.student.service.StudentService;
import com.college.erp.module.transport.domain.Route;
import com.college.erp.module.transport.domain.StudentRouteMapping;
import com.college.erp.module.transport.domain.StudentRouteMappingRepository;
import com.college.erp.module.transport.dto.AssignRouteRequest;
import com.college.erp.module.transport.dto.StudentRouteMappingDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** 04-database-schema.md §14.2. Named to match the module's other *ServiceTest files. */
@ExtendWith(MockitoExtension.class)
class StudentRouteMappingServiceTest {

    @Mock
    private StudentRouteMappingRepository studentRouteMappingRepository;
    @Mock
    private StudentService studentService;
    @Mock
    private RouteService routeService;
    @Mock
    private VehicleService vehicleService;
    @Mock
    private AuditLogWriter auditLogWriter;

    private StudentRouteMappingService studentRouteMappingService;

    private Student buildStudent() {
        Student student = Student.builder().enrollmentNumber("2024CSE001").firstName("Arjun")
                .lastName("Kumar").build();
        student.setId(UUID.randomUUID());
        return student;
    }

    private Route buildRoute() {
        Route route = Route.builder().name("Route 1").build();
        route.setId(UUID.randomUUID());
        return route;
    }

    @Test
    @DisplayName("assign() — a student with no active mapping is assigned successfully")
    void assign_noExistingMapping_succeeds() {
        // ARRANGE
        studentRouteMappingService = new StudentRouteMappingService(studentRouteMappingRepository, studentService,
                routeService, vehicleService, auditLogWriter);
        Student student = buildStudent();
        Route route = buildRoute();
        when(studentRouteMappingRepository.existsByStudentIdAndActiveTrueAndDeletedFalse(student.getId()))
                .thenReturn(false);
        when(studentService.getStudentEntity(student.getId())).thenReturn(student);
        when(routeService.getRouteEntity(route.getId())).thenReturn(route);
        when(studentRouteMappingRepository.save(any(StudentRouteMapping.class))).thenAnswer(inv -> {
            StudentRouteMapping mapping = inv.getArgument(0);
            mapping.setId(UUID.randomUUID());
            return mapping;
        });

        AssignRouteRequest request = new AssignRouteRequest();
        request.setStudentId(student.getId());
        request.setRouteId(route.getId());
        request.setStopName("Main Gate");

        // ACT
        StudentRouteMappingDto dto = studentRouteMappingService.assign(request);

        // ASSERT
        assertThat(dto.isActive()).isTrue();
        assertThat(dto.getStopName()).isEqualTo("Main Gate");
    }

    @Test
    @DisplayName("assign() — a student with an existing active mapping is rejected")
    void assign_existingActiveMapping_throws() {
        // ARRANGE
        studentRouteMappingService = new StudentRouteMappingService(studentRouteMappingRepository, studentService,
                routeService, vehicleService, auditLogWriter);
        UUID studentId = UUID.randomUUID();
        when(studentRouteMappingRepository.existsByStudentIdAndActiveTrueAndDeletedFalse(studentId))
                .thenReturn(true);

        AssignRouteRequest request = new AssignRouteRequest();
        request.setStudentId(studentId);
        request.setRouteId(UUID.randomUUID());

        // ACT / ASSERT
        assertThatThrownBy(() -> studentRouteMappingService.assign(request))
                .isInstanceOf(BusinessRuleViolationException.class);
    }
}
