package com.college.erp.module.library.domain;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookTitleRepository extends JpaRepository<BookTitle, UUID> {

    Optional<BookTitle> findByIdAndDeletedFalse(UUID id);

    @Query("""
        SELECT bt FROM BookTitle bt
        WHERE bt.deleted = false
        AND (:q IS NULL OR :q = '' OR
             LOWER(bt.title) LIKE LOWER(CONCAT('%', :q, '%')) OR
             LOWER(bt.author) LIKE LOWER(CONCAT('%', :q, '%')) OR
             bt.isbn = :q)
        """)
    Page<BookTitle> search(@Param("q") String query, Pageable pageable);
}
