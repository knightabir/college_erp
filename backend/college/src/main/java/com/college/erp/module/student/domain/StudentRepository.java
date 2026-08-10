package com.college.erp.module.student.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentRepository extends JpaRepository<Student, UUID> {

    Optional<Student> findByIdAndDeletedFalse(UUID id);

    Optional<Student> findByUserIdAndDeletedFalse(UUID userId);

    Optional<Student> findByEnrollmentNumberAndDeletedFalse(String enrollmentNumber);

    boolean existsByEnrollmentNumberAndDeletedFalse(String enrollmentNumber);

    // Section/batch membership lives on the current enrollment, not on Student itself — join
    // through enrollments filtered to is_current = true (04-database-schema.md §7.2 key rule).
    @Query("""
        SELECT s FROM Student s
        JOIN Enrollment e ON e.student = s
        WHERE e.section.id = :sectionId
        AND e.current = true
        AND e.deleted = false
        AND s.deleted = false
        ORDER BY s.lastName, s.firstName
        """)
    Page<Student> findBySectionId(@Param("sectionId") UUID sectionId, Pageable pageable);

    @Query("""
        SELECT s FROM Student s
        JOIN Enrollment e ON e.student = s
        WHERE e.batch.id = :batchId
        AND e.current = true
        AND e.deleted = false
        AND s.deleted = false
        ORDER BY s.lastName, s.firstName
        """)
    Page<Student> findByBatchId(@Param("batchId") UUID batchId, Pageable pageable);

    /** Unpaged variant — module.examination needs the whole batch roster at once to generate
     *  hall tickets in one pass, not a page at a time. */
    @Query("""
        SELECT s FROM Student s
        JOIN Enrollment e ON e.student = s
        WHERE e.batch.id = :batchId
        AND e.current = true
        AND e.deleted = false
        AND s.deleted = false
        ORDER BY s.lastName, s.firstName
        """)
    List<Student> findAllByBatchId(@Param("batchId") UUID batchId);
}
