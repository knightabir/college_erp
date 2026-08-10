package com.college.erp.module.communication.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoticeRepository extends JpaRepository<Notice, UUID> {

    Optional<Notice> findByIdAndDeletedFalse(UUID id);

    @Query("""
        SELECT n FROM Notice n
        WHERE n.deleted = false
        AND n.publishedAt IS NOT NULL AND n.publishedAt <= :now
        AND (n.expiresAt IS NULL OR n.expiresAt > :now)
        AND (n.targetType = com.college.erp.module.communication.domain.NoticeTargetType.ALL
             OR n.targetType = com.college.erp.module.communication.domain.NoticeTargetType.ROLE
             OR (n.targetType = com.college.erp.module.communication.domain.NoticeTargetType.DEPARTMENT
                 AND n.targetId = :departmentId)
             OR (n.targetType = com.college.erp.module.communication.domain.NoticeTargetType.BATCH
                 AND n.targetId = :batchId))
        ORDER BY n.pinned DESC, n.publishedAt DESC
        """)
    List<Notice> findRelevant(@Param("now") Instant now, @Param("departmentId") UUID departmentId,
            @Param("batchId") UUID batchId);
}
