package com.college.erp.module.transport.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.BusinessRuleViolationException;
import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.module.student.domain.Student;
import com.college.erp.module.student.service.StudentService;
import com.college.erp.module.transport.domain.Route;
import com.college.erp.module.transport.domain.StudentRouteMapping;
import com.college.erp.module.transport.domain.StudentRouteMappingRepository;
import com.college.erp.module.transport.domain.Vehicle;
import com.college.erp.module.transport.dto.AssignRouteRequest;
import com.college.erp.module.transport.dto.StudentRouteMappingDto;

import lombok.RequiredArgsConstructor;

/** 04-database-schema.md §14.2 — see the module's CLAUDE.md deviations note (no SRS/API basis). */
@Service
@RequiredArgsConstructor
public class StudentRouteMappingService {

    private static final String MODULE = "TRANSPORT";

    private final StudentRouteMappingRepository studentRouteMappingRepository;
    private final StudentService studentService;
    private final RouteService routeService;
    private final VehicleService vehicleService;
    private final AuditLogWriter auditLogWriter;

    @Transactional
    public StudentRouteMappingDto assign(AssignRouteRequest request) {
        if (studentRouteMappingRepository.existsByStudentIdAndActiveTrueAndDeletedFalse(request.getStudentId())) {
            throw new BusinessRuleViolationException("Student already has an active route assignment");
        }
        Student student = studentService.getStudentEntity(request.getStudentId());
        Route route = routeService.getRouteEntity(request.getRouteId());
        Vehicle vehicle = request.getVehicleId() != null
                ? vehicleService.getVehicleEntity(request.getVehicleId())
                : null;

        StudentRouteMapping mapping = StudentRouteMapping.builder()
                .student(student)
                .route(route)
                .vehicle(vehicle)
                .stopName(request.getStopName())
                .build();
        StudentRouteMapping saved = studentRouteMappingRepository.save(mapping);

        auditLogWriter.write(AuditAction.CREATE, MODULE, "StudentRouteMapping", saved.getId().toString(),
                "Assigned " + student.getFullName() + " to route " + route.getName());

        return StudentRouteMappingDto.from(saved);
    }

    @Transactional
    public void deactivate(UUID id) {
        StudentRouteMapping mapping = studentRouteMappingRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("StudentRouteMapping", id));
        mapping.setActive(false);
        studentRouteMappingRepository.save(mapping);
        auditLogWriter.write(AuditAction.UPDATE, MODULE, "StudentRouteMapping", id.toString(), "Deactivated");
    }

    @Transactional(readOnly = true)
    public List<StudentRouteMappingDto> listByRoute(UUID routeId) {
        return studentRouteMappingRepository.findByRouteIdAndActiveTrueAndDeletedFalse(routeId).stream()
                .map(StudentRouteMappingDto::from)
                .toList();
    }
}
