package com.college.erp.module.library.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BookCopyRepository extends JpaRepository<BookCopy, UUID> {

    Optional<BookCopy> findByIdAndDeletedFalse(UUID id);

    Optional<BookCopy> findByBarcodeAndDeletedFalse(String barcode);

    List<BookCopy> findByTitleIdAndDeletedFalse(UUID titleId);

    boolean existsByBarcode(String barcode);

    boolean existsByAccessionNumber(String accessionNumber);
}
