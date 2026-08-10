package com.college.erp.module.examination.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MarksEntryRepository extends JpaRepository<MarksEntry, UUID> {

    Optional<MarksEntry> findByIdAndDeletedFalse(UUID id);

    Optional<MarksEntry> findByExamIdAndOfferingIdAndStudentIdAndDeletedFalse(
            UUID examId, UUID offeringId, UUID studentId);

    List<MarksEntry> findByExamIdAndDeletedFalse(UUID examId);

    @Query("""
        SELECT me FROM MarksEntry me
        WHERE me.student.id = :studentId
        AND me.offering.term.id = :termId
        AND me.status = com.college.erp.module.examination.domain.MarksStatus.PUBLISHED
        AND me.deleted = false
        """)
    List<MarksEntry> findPublishedByStudentAndTerm(@Param("studentId") UUID studentId, @Param("termId") UUID termId);
}
