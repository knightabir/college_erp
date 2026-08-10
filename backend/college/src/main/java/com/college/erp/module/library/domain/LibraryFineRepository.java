package com.college.erp.module.library.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LibraryFineRepository extends JpaRepository<LibraryFine, UUID> {

    Optional<LibraryFine> findByIdAndDeletedFalse(UUID id);

    List<LibraryFine> findByUserIdAndDeletedFalse(UUID userId);

    List<LibraryFine> findByUserIdAndStatusInAndDeletedFalse(UUID userId, List<FineStatus> statuses);
}
