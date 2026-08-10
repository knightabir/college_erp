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
import com.college.erp.module.academic.domain.Department;
import com.college.erp.module.academic.domain.DepartmentRepository;
import com.college.erp.module.academic.dto.CreateDepartmentRequest;
import com.college.erp.module.academic.dto.DepartmentDto;
import com.college.erp.tenant.TenantContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepartmentService {

    private static final String MODULE = "ACADEMIC";

    private final DepartmentRepository departmentRepository;
    private final AuditLogWriter auditLogWriter;

    @Transactional
    public DepartmentDto create(CreateDepartmentRequest request) {
        String code = request.getCode().toUpperCase();
        if (departmentRepository.existsByCodeAndDeletedFalse(code)) {
            throw new DuplicateResourceException("Department with code '" + code + "' already exists");
        }

        Department department = Department.builder()
                .name(request.getName())
                .code(code)
                .description(request.getDescription())
                .hodUserId(request.getHodUserId())
                .contactEmail(request.getContactEmail())
                .contactPhone(request.getContactPhone())
                .build();
        department.setTenantId(TenantContext.getCurrentTenantId());

        Department saved = departmentRepository.save(department);

        auditLogWriter.write(AuditAction.CREATE, MODULE, "Department", saved.getId().toString(),
                "Created: " + saved.getName());
        log.info("Department created: [{}] {}", saved.getCode(), saved.getName());

        return DepartmentDto.from(saved);
    }

    @Transactional
    public DepartmentDto update(UUID id, CreateDepartmentRequest request) {
        Department department = getEntity(id);
        String newCode = request.getCode().toUpperCase();

        if (!department.getCode().equals(newCode) && departmentRepository.existsByCodeAndDeletedFalse(newCode)) {
            throw new DuplicateResourceException("Department with code '" + newCode + "' already exists");
        }

        department.setName(request.getName());
        department.setCode(newCode);
        department.setDescription(request.getDescription());
        department.setHodUserId(request.getHodUserId());
        department.setContactEmail(request.getContactEmail());
        department.setContactPhone(request.getContactPhone());

        Department saved = departmentRepository.save(department);
        auditLogWriter.write(AuditAction.UPDATE, MODULE, "Department", saved.getId().toString(),
                "Updated: " + saved.getName());

        return DepartmentDto.from(saved);
    }

    @Transactional
    public void deactivate(UUID id) {
        Department department = getEntity(id);
        department.setActive(false);
        departmentRepository.save(department);
        auditLogWriter.write(AuditAction.UPDATE, MODULE, "Department", id.toString(),
                "Deactivated: " + department.getName());
    }

    @Transactional(readOnly = true)
    public DepartmentDto getById(UUID id) {
        return DepartmentDto.from(getEntity(id));
    }

    /** For other modules that need the managed entity — see BatchService.getBatchEntity javadoc. */
    @Transactional(readOnly = true)
    public Department getDepartmentEntity(UUID id) {
        return getEntity(id);
    }

    @Transactional(readOnly = true)
    public Page<DepartmentDto> list(Pageable pageable) {
        return departmentRepository.findByDeletedFalse(pageable).map(DepartmentDto::from);
    }

    @Transactional(readOnly = true)
    public List<DepartmentDto> listActive() {
        return departmentRepository.findByActiveTrueAndDeletedFalse().stream()
                .map(DepartmentDto::from)
                .toList();
    }

    private Department getEntity(UUID id) {
        return departmentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", id));
    }
}
