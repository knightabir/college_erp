package com.college.erp.module.academic.domain;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FacultySubjectMappingRepository extends JpaRepository<FacultySubjectMapping, UUID> {

    List<FacultySubjectMapping> findBySectionIdAndDeletedFalse(UUID sectionId);

    List<FacultySubjectMapping> findByFacultyUserIdAndDeletedFalse(UUID facultyUserId);

    @Query("""
        SELECT fsm FROM FacultySubjectMapping fsm
        WHERE fsm.offering.id = :offeringId
        AND fsm.section.id = :sectionId
        AND fsm.primary = true
        AND fsm.active = true
        AND fsm.deleted = false
        """)
    List<FacultySubjectMapping> findActivePrimaryFor(
            @Param("offeringId") UUID offeringId, @Param("sectionId") UUID sectionId);

    boolean existsByOfferingIdAndFacultyUserIdAndActiveTrueAndDeletedFalse(UUID offeringId, UUID facultyUserId);
}
