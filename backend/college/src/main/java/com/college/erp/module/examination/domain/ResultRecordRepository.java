package com.college.erp.module.examination.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ResultRecordRepository extends JpaRepository<ResultRecord, UUID> {

    Optional<ResultRecord> findByStudentIdAndTermIdAndDeletedFalse(UUID studentId, UUID termId);

    List<ResultRecord> findByStudentIdAndDeletedFalse(UUID studentId);
}
