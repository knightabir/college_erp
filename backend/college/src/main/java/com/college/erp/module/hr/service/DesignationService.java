package com.college.erp.module.hr.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.module.hr.domain.Designation;
import com.college.erp.module.hr.domain.DesignationRepository;
import com.college.erp.module.hr.dto.CreateDesignationRequest;
import com.college.erp.module.hr.dto.DesignationDto;

import lombok.RequiredArgsConstructor;

/** 04-database-schema.md §13.1. */
@Service
@RequiredArgsConstructor
public class DesignationService {

    private static final String MODULE = "HR";

    private final DesignationRepository designationRepository;
    private final AuditLogWriter auditLogWriter;

    @Transactional
    public DesignationDto create(CreateDesignationRequest request) {
        Designation designation = Designation.builder()
                .name(request.getName())
                .grade(request.getGrade())
                .build();
        Designation saved = designationRepository.save(designation);

        auditLogWriter.write(AuditAction.CREATE, MODULE, "Designation", saved.getId().toString(),
                "Created: " + saved.getName());

        return DesignationDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<DesignationDto> list() {
        return designationRepository.findByDeletedFalse().stream()
                .map(DesignationDto::from)
                .toList();
    }

    /** For StaffService — see BatchService.getBatchEntity javadoc. */
    @Transactional(readOnly = true)
    public Designation getDesignationEntity(UUID id) {
        return designationRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Designation", id));
    }
}
