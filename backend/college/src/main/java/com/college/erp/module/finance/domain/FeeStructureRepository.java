package com.college.erp.module.finance.domain;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeeStructureRepository extends JpaRepository<FeeStructure, UUID> {

    List<FeeStructure> findByCourseIdAndDeletedFalse(UUID courseId);

    boolean existsByCourseIdAndBatchIdAndFeeHeadIdAndTermNumberAndDeletedFalse(
            UUID courseId, UUID batchId, UUID feeHeadId, int termNumber);

    /** Applicable fee structures for one invoice: course-wide (batch IS NULL) plus this specific
     *  batch's overrides — both may contribute (03-srs.md [FIN-001]/[FIN-002]). */
    @Query("""
        SELECT fs FROM FeeStructure fs
        WHERE fs.course.id = :courseId
        AND (fs.batch IS NULL OR fs.batch.id = :batchId)
        AND fs.termNumber = :termNumber
        AND fs.active = true
        AND fs.deleted = false
        """)
    List<FeeStructure> findApplicable(
            @Param("courseId") UUID courseId, @Param("batchId") UUID batchId, @Param("termNumber") int termNumber);
}
