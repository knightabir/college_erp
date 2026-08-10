package com.college.erp.module.academic.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TermRepository extends JpaRepository<Term, UUID> {

    List<Term> findByAcademicYearIdAndDeletedFalse(UUID academicYearId);

    Optional<Term> findByIdAndDeletedFalse(UUID id);

    /** Used by module.student to resolve "the batch's current term" at enrollment time — a
     *  batch only stores currentTermNumber + its academic year, not a direct term_id. */
    Optional<Term> findByAcademicYearIdAndTermNumberAndDeletedFalse(UUID academicYearId, int termNumber);
}
