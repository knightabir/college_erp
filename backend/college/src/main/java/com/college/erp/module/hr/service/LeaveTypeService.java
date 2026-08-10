package com.college.erp.module.hr.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.DuplicateResourceException;
import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.module.hr.domain.LeaveType;
import com.college.erp.module.hr.domain.LeaveTypeRepository;
import com.college.erp.module.hr.dto.CreateLeaveTypeRequest;
import com.college.erp.module.hr.dto.LeaveTypeDto;

import lombok.RequiredArgsConstructor;

/** 04-database-schema.md §13.3, 03-srs.md [HR-002]. */
@Service
@RequiredArgsConstructor
public class LeaveTypeService {

    private static final String MODULE = "HR";

    private final LeaveTypeRepository leaveTypeRepository;
    private final AuditLogWriter auditLogWriter;

    @Transactional
    public LeaveTypeDto create(CreateLeaveTypeRequest request) {
        String code = request.getCode().toUpperCase();
        if (leaveTypeRepository.existsByCodeAndDeletedFalse(code)) {
            throw new DuplicateResourceException("Leave type with code '" + code + "' already exists");
        }

        LeaveType leaveType = LeaveType.builder()
                .code(code)
                .name(request.getName())
                .daysPerYear(request.getDaysPerYear())
                .paid(request.isPaid())
                .build();
        LeaveType saved = leaveTypeRepository.save(leaveType);

        auditLogWriter.write(AuditAction.CREATE, MODULE, "LeaveType", saved.getId().toString(),
                "Created: " + saved.getName());

        return LeaveTypeDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<LeaveTypeDto> list() {
        return leaveTypeRepository.findByDeletedFalse().stream()
                .map(LeaveTypeDto::from)
                .toList();
    }

    /** For LeaveBalanceService/LeaveRequestService — see BatchService.getBatchEntity javadoc. */
    @Transactional(readOnly = true)
    public LeaveType getLeaveTypeEntity(UUID id) {
        return leaveTypeRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveType", id));
    }

    @Transactional(readOnly = true)
    public List<LeaveType> listActiveEntities() {
        return leaveTypeRepository.findByDeletedFalse();
    }
}
