package com.college.erp.module.academic.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectOfferingRepository extends JpaRepository<SubjectOffering, UUID> {

    Optional<SubjectOffering> findByIdAndDeletedFalse(UUID id);

    List<SubjectOffering> findByBatchIdAndTermIdAndDeletedFalse(UUID batchId, UUID termId);

    boolean existsByBatchIdAndSubjectIdAndTermIdAndDeletedFalse(UUID batchId, UUID subjectId, UUID termId);

    /** AttendanceShortfallScanJob: every offering to sweep for defaulters. */
    List<SubjectOffering> findByDeletedFalse();
}
