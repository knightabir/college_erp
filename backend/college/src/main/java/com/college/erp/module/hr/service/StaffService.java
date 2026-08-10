package com.college.erp.module.hr.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.DuplicateResourceException;
import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.module.academic.domain.Department;
import com.college.erp.module.academic.service.DepartmentService;
import com.college.erp.module.hr.domain.Designation;
import com.college.erp.module.hr.domain.StaffProfile;
import com.college.erp.module.hr.domain.StaffProfileRepository;
import com.college.erp.module.hr.dto.CreateStaffProfileRequest;
import com.college.erp.module.hr.dto.StaffProfileDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 03-srs.md [HR-001]. */
@Slf4j
@Service
@RequiredArgsConstructor
public class StaffService {

    private static final String MODULE = "HR";

    private final StaffProfileRepository staffProfileRepository;
    private final DepartmentService departmentService;
    private final DesignationService designationService;
    private final AuditLogWriter auditLogWriter;

    @Transactional
    public StaffProfileDto create(CreateStaffProfileRequest request) {
        if (staffProfileRepository.existsByEmployeeNumber(request.getEmployeeNumber())) {
            throw new DuplicateResourceException(
                    "Staff profile with employee number '" + request.getEmployeeNumber() + "' already exists");
        }
        if (staffProfileRepository.existsByUserIdAndDeletedFalse(request.getUserId())) {
            throw new DuplicateResourceException("This user already has a staff profile");
        }

        Department department = departmentService.getDepartmentEntity(request.getDepartmentId());
        Designation designation = request.getDesignationId() != null
                ? designationService.getDesignationEntity(request.getDesignationId())
                : null;

        StaffProfile profile = StaffProfile.builder()
                .userId(request.getUserId())
                .employeeNumber(request.getEmployeeNumber())
                .department(department)
                .designation(designation)
                .staffType(request.getStaffType())
                .joiningDate(request.getJoiningDate())
                .qualification(request.getQualification())
                .specialization(request.getSpecialization())
                .experienceYears(request.getExperienceYears())
                .bankAccountNumber(request.getBankAccountNumber())
                .ifscCode(request.getIfscCode())
                .panNumber(request.getPanNumber())
                .pfNumber(request.getPfNumber())
                .build();

        StaffProfile saved = staffProfileRepository.save(profile);
        auditLogWriter.write(AuditAction.CREATE, MODULE, "StaffProfile", saved.getId().toString(),
                "Created staff profile " + saved.getEmployeeNumber());
        log.info("Staff profile created: [{}]", saved.getEmployeeNumber());

        return StaffProfileDto.from(saved);
    }

    @Transactional(readOnly = true)
    public StaffProfileDto getById(UUID id) {
        return StaffProfileDto.from(getEntity(id));
    }

    @Transactional(readOnly = true)
    public StaffProfileDto getByUserId(UUID userId) {
        return StaffProfileDto.from(staffProfileRepository.findByUserIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("StaffProfile for user", userId)));
    }

    @Transactional(readOnly = true)
    public List<StaffProfileDto> listByDepartment(UUID departmentId) {
        return staffProfileRepository.findByDepartmentIdAndDeletedFalse(departmentId).stream()
                .map(StaffProfileDto::from)
                .toList();
    }

    private StaffProfile getEntity(UUID id) {
        return staffProfileRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("StaffProfile", id));
    }
}
