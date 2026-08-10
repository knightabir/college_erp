package com.college.erp.module.admissions.domain;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MeritListEntryRepository extends JpaRepository<MeritListEntry, UUID> {

    List<MeritListEntry> findByMeritListIdOrderByRankAsc(UUID meritListId);
}
