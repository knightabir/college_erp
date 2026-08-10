package com.college.erp.module.student.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentDocumentRepository extends JpaRepository<StudentDocument, UUID> {

    List<StudentDocument> findByStudentIdAndDeletedFalse(UUID studentId);

    Optional<StudentDocument> findByIdAndDeletedFalse(UUID id);
}
