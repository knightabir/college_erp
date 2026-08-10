package com.college.erp.module.academic.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.module.academic.domain.Section;
import com.college.erp.module.academic.domain.SectionRepository;
import com.college.erp.module.academic.dto.SectionDto;

import lombok.RequiredArgsConstructor;

/**
 * Section create/list is nested under batches (05-api-documentation.md §3 "Batches" —
 * POST/GET /batches/{id}/sections, handled by BatchService/BatchController). This service only
 * covers the direct-by-id read a standalone SectionController needs, plus the entity accessor
 * other modules use to build associations (see BatchService.getBatchEntity javadoc).
 */
@Service
@RequiredArgsConstructor
public class SectionService {

    private final SectionRepository sectionRepository;

    @Transactional(readOnly = true)
    public SectionDto getById(UUID id) {
        return SectionDto.from(getEntity(id));
    }

    @Transactional(readOnly = true)
    public Section getSectionEntity(UUID id) {
        return getEntity(id);
    }

    private Section getEntity(UUID id) {
        return sectionRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Section", id));
    }
}
