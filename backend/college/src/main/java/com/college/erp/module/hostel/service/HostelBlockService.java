package com.college.erp.module.hostel.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.module.hostel.domain.HostelBlock;
import com.college.erp.module.hostel.domain.HostelBlockRepository;
import com.college.erp.module.hostel.dto.CreateHostelBlockRequest;
import com.college.erp.module.hostel.dto.HostelBlockDto;

import lombok.RequiredArgsConstructor;

/** 04-database-schema.md §14.1 — see the module's CLAUDE.md deviations note (no SRS/API basis). */
@Service
@RequiredArgsConstructor
public class HostelBlockService {

    private static final String MODULE = "HOSTEL";

    private final HostelBlockRepository hostelBlockRepository;
    private final AuditLogWriter auditLogWriter;

    @Transactional
    public HostelBlockDto create(CreateHostelBlockRequest request) {
        HostelBlock block = HostelBlock.builder()
                .name(request.getName())
                .gender(request.getGender())
                .wardenId(request.getWardenId())
                .capacity(request.getCapacity())
                .build();
        HostelBlock saved = hostelBlockRepository.save(block);

        auditLogWriter.write(AuditAction.CREATE, MODULE, "HostelBlock", saved.getId().toString(),
                "Created: " + saved.getName());

        return HostelBlockDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<HostelBlockDto> list() {
        return hostelBlockRepository.findByDeletedFalse().stream()
                .map(HostelBlockDto::from)
                .toList();
    }

    /** For RoomService — see BatchService.getBatchEntity javadoc. */
    @Transactional(readOnly = true)
    public HostelBlock getBlockEntity(UUID id) {
        return hostelBlockRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("HostelBlock", id));
    }
}
