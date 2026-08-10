package com.college.erp.module.academic.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.college.erp.common.exception.BusinessRuleViolationException;
import com.college.erp.module.academic.domain.FacultySubjectMappingRepository;

import lombok.RequiredArgsConstructor;

/**
 * The documented schema has no day/time-slot columns anywhere in the academic module (see
 * FacultySubjectMapping javadoc) — genuine "double-booked at the same time" conflict detection
 * isn't possible with the data available. The one conflict this data DOES support: a
 * subject-offering/section pair should have exactly one active primary faculty at a time (a
 * second primary would make "who is responsible for this section's marks/attendance" ambiguous).
 * Extend this once the schema gains scheduling data.
 */
@Service
@RequiredArgsConstructor
public class ConflictDetectionService {

    private final FacultySubjectMappingRepository facultySubjectMappingRepository;

    public void checkNoDuplicatePrimary(UUID offeringId, UUID sectionId) {
        if (!facultySubjectMappingRepository.findActivePrimaryFor(offeringId, sectionId).isEmpty()) {
            throw new BusinessRuleViolationException(
                    "This section already has an active primary faculty for this subject offering");
        }
    }
}
