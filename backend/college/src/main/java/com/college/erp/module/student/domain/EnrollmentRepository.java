package com.college.erp.module.student.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {

    Optional<Enrollment> findByStudentIdAndCurrentTrueAndDeletedFalse(UUID studentId);

    @Query("""
        SELECT e FROM Enrollment e
        WHERE e.student.id = :studentId
        AND e.deleted = false
        ORDER BY e.enrollmentDate DESC, e.createdAt DESC
        """)
    List<Enrollment> findHistoryByStudentId(@Param("studentId") UUID studentId);
}
