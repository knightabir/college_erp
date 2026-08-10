package com.college.erp.module.examination.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamRepository extends JpaRepository<Exam, UUID> {

    Optional<Exam> findByIdAndDeletedFalse(UUID id);

    List<Exam> findByBatchIdAndDeletedFalse(UUID batchId);
}
