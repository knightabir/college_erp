package com.college.erp.module.admissions.domain;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationWorkflowStepRepository extends JpaRepository<ApplicationWorkflowStep, UUID> {

    List<ApplicationWorkflowStep> findByApplicationIdOrderByActionedAtAsc(UUID applicationId);
}
