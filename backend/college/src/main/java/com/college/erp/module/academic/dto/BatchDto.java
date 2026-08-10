package com.college.erp.module.academic.dto;

import java.util.UUID;

import com.college.erp.module.academic.domain.Batch;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BatchDto {

    private UUID id;
    private UUID courseId;
    private String courseName;
    private UUID academicYearId;
    private String label;
    private int intakeYear;
    private int currentTermNumber;
    private boolean active;

    public static BatchDto from(Batch batch) {
        return BatchDto.builder()
                .id(batch.getId())
                .courseId(batch.getCourse().getId())
                .courseName(batch.getCourse().getName())
                .academicYearId(batch.getAcademicYear().getId())
                .label(batch.getLabel())
                .intakeYear(batch.getIntakeYear())
                .currentTermNumber(batch.getCurrentTermNumber())
                .active(batch.isActive())
                .build();
    }
}
