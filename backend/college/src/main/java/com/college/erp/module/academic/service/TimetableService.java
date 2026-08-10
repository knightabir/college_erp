package com.college.erp.module.academic.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.DuplicateResourceException;
import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.module.academic.domain.FacultySubjectMapping;
import com.college.erp.module.academic.domain.FacultySubjectMappingRepository;
import com.college.erp.module.academic.domain.Section;
import com.college.erp.module.academic.domain.SectionRepository;
import com.college.erp.module.academic.domain.SubjectOffering;
import com.college.erp.module.academic.domain.SubjectOfferingRepository;
import com.college.erp.module.academic.dto.AssignFacultyRequest;
import com.college.erp.module.academic.dto.TimetableEntryDto;
import com.college.erp.tenant.TenantContext;

import lombok.RequiredArgsConstructor;

/** See TimetableEntryDto / FacultySubjectMapping / ConflictDetectionService javadocs for what
 *  "timetable" means given the documented schema has no scheduling/time-slot data. */
@Service
@RequiredArgsConstructor
public class TimetableService {

    private static final String MODULE = "ACADEMIC";

    private final FacultySubjectMappingRepository facultySubjectMappingRepository;
    private final SubjectOfferingRepository subjectOfferingRepository;
    private final SectionRepository sectionRepository;
    private final ConflictDetectionService conflictDetectionService;
    private final AuditLogWriter auditLogWriter;

    @Transactional
    public TimetableEntryDto assignFaculty(AssignFacultyRequest request) {
        SubjectOffering offering = subjectOfferingRepository.findByIdAndDeletedFalse(request.getOfferingId())
                .orElseThrow(() -> new ResourceNotFoundException("SubjectOffering", request.getOfferingId()));
        Section section = sectionRepository.findByIdAndDeletedFalse(request.getSectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Section", request.getSectionId()));

        if (request.isPrimary()) {
            conflictDetectionService.checkNoDuplicatePrimary(offering.getId(), section.getId());
        }

        FacultySubjectMapping mapping = FacultySubjectMapping.builder()
                .offering(offering)
                .section(section)
                .facultyUserId(request.getFacultyUserId())
                .primary(request.isPrimary())
                .build();
        mapping.setTenantId(TenantContext.getCurrentTenantId());

        FacultySubjectMapping saved;
        try {
            saved = facultySubjectMappingRepository.save(mapping);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new DuplicateResourceException("This faculty is already assigned to this offering/section");
        }

        auditLogWriter.write(AuditAction.CREATE, MODULE, "FacultySubjectMapping", saved.getId().toString(),
                "Assigned faculty to " + offering.getSubject().getName() + " / section " + section.getName());

        return TimetableEntryDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<TimetableEntryDto> getTimetableForSection(UUID sectionId) {
        return facultySubjectMappingRepository.findBySectionIdAndDeletedFalse(sectionId).stream()
                .map(TimetableEntryDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TimetableEntryDto> getTimetableForFaculty(UUID facultyUserId) {
        return facultySubjectMappingRepository.findByFacultyUserIdAndDeletedFalse(facultyUserId).stream()
                .map(TimetableEntryDto::from)
                .toList();
    }

    /** Used by module.examination to enforce 05-api-documentation.md §6's "FACULTY (assigned to
     *  this offering)" auth note on marks entry — not enforceable by role alone. */
    @Transactional(readOnly = true)
    public boolean isFacultyAssignedToOffering(UUID facultyUserId, UUID offeringId) {
        return facultySubjectMappingRepository
                .existsByOfferingIdAndFacultyUserIdAndActiveTrueAndDeletedFalse(offeringId, facultyUserId);
    }
}
