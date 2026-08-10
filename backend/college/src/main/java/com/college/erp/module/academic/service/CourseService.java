package com.college.erp.module.academic.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.DuplicateResourceException;
import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.module.academic.domain.Course;
import com.college.erp.module.academic.domain.CourseRepository;
import com.college.erp.module.academic.domain.Department;
import com.college.erp.module.academic.domain.DepartmentRepository;
import com.college.erp.module.academic.dto.CourseDto;
import com.college.erp.module.academic.dto.CreateCourseRequest;
import com.college.erp.tenant.TenantContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseService {

    private static final String MODULE = "ACADEMIC";

    private final CourseRepository courseRepository;
    private final DepartmentRepository departmentRepository;
    private final AuditLogWriter auditLogWriter;

    @Transactional
    public CourseDto create(CreateCourseRequest request) {
        Department department = departmentRepository.findByIdAndDeletedFalse(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department", request.getDepartmentId()));

        String code = request.getCode().toUpperCase();
        if (courseRepository.existsByCodeAndDeletedFalse(code)) {
            throw new DuplicateResourceException("Course with code '" + code + "' already exists");
        }

        Course course = Course.builder()
                .department(department)
                .name(request.getName())
                .code(code)
                .level(request.getLevel())
                .durationYears(request.getDurationYears())
                .totalCredits(request.getTotalCredits())
                .intakeCapacity(request.getIntakeCapacity())
                .description(request.getDescription())
                .build();
        course.setTenantId(TenantContext.getCurrentTenantId());

        Course saved = courseRepository.save(course);
        auditLogWriter.write(AuditAction.CREATE, MODULE, "Course", saved.getId().toString(),
                "Created: " + saved.getName());
        log.info("Course created: [{}] {}", saved.getCode(), saved.getName());

        return CourseDto.from(saved);
    }

    @Transactional(readOnly = true)
    public CourseDto getById(UUID id) {
        return CourseDto.from(getEntity(id));
    }

    @Transactional(readOnly = true)
    public Page<CourseDto> list(Pageable pageable) {
        return courseRepository.findByDeletedFalse(pageable).map(CourseDto::from);
    }

    @Transactional(readOnly = true)
    public List<CourseDto> listByDepartment(UUID departmentId) {
        return courseRepository.findByDepartmentIdAndDeletedFalse(departmentId).stream()
                .map(CourseDto::from)
                .toList();
    }

    /** For other modules that need the managed entity — see BatchService.getBatchEntity javadoc. */
    @Transactional(readOnly = true)
    public Course getCourseEntity(UUID id) {
        return getEntity(id);
    }

    private Course getEntity(UUID id) {
        return courseRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course", id));
    }
}
