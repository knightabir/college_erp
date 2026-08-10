package com.college.erp.module.examination.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RevaluationRequestRepository extends JpaRepository<RevaluationRequest, UUID> {

    Optional<RevaluationRequest> findByIdAndDeletedFalse(UUID id);

    List<RevaluationRequest> findByStudentIdAndDeletedFalse(UUID studentId);

    boolean existsByMarksEntryIdAndDeletedFalse(UUID marksEntryId);
}
