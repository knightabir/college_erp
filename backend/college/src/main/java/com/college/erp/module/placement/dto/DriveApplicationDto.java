package com.college.erp.module.placement.dto;

import java.time.Instant;
import java.util.UUID;

import com.college.erp.module.placement.domain.DriveApplication;
import com.college.erp.module.placement.domain.DriveApplicationStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DriveApplicationDto {

    private UUID id;
    private UUID driveId;
    private String driveTitle;
    private UUID studentId;
    private String studentName;
    private DriveApplicationStatus status;
    private Instant appliedAt;

    public static DriveApplicationDto from(DriveApplication application) {
        return DriveApplicationDto.builder()
                .id(application.getId())
                .driveId(application.getDrive().getId())
                .driveTitle(application.getDrive().getTitle())
                .studentId(application.getStudent().getId())
                .studentName(application.getStudent().getFullName())
                .status(application.getStatus())
                .appliedAt(application.getAppliedAt())
                .build();
    }
}
