package com.college.erp.module.admissions.dto;

import java.time.Instant;
import java.util.UUID;

import com.college.erp.module.admissions.domain.ApplicationStatus;
import com.college.erp.module.admissions.domain.ApplicationWorkflowStep;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApplicationWorkflowStepDto {

    private UUID id;
    private ApplicationStatus fromStatus;
    private ApplicationStatus toStatus;
    private String remarks;
    private UUID actionedBy;
    private Instant actionedAt;

    public static ApplicationWorkflowStepDto from(ApplicationWorkflowStep step) {
        return ApplicationWorkflowStepDto.builder()
                .id(step.getId())
                .fromStatus(step.getFromStatus())
                .toStatus(step.getToStatus())
                .remarks(step.getRemarks())
                .actionedBy(step.getActionedBy())
                .actionedAt(step.getActionedAt())
                .build();
    }
}
