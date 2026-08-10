package com.college.erp.module.student.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.college.erp.module.student.domain.Enrollment;
import com.college.erp.module.student.domain.EnrollmentStatus;

import lombok.Builder;
import lombok.Getter;

/** 05-api-documentation.md §4 GET /students/{id}/enrollment response shape. */
@Getter
@Builder
public class EnrollmentDto {

    private UUID id;
    private UUID studentId;
    private UUID batchId;
    private UUID sectionId;
    private UUID termId;
    private LocalDate enrollmentDate;
    private EnrollmentStatus status;
    private boolean current;
    private String remarks;

    public static EnrollmentDto from(Enrollment enrollment) {
        return EnrollmentDto.builder()
                .id(enrollment.getId())
                .studentId(enrollment.getStudent().getId())
                .batchId(enrollment.getBatch().getId())
                .sectionId(enrollment.getSection().getId())
                .termId(enrollment.getTerm().getId())
                .enrollmentDate(enrollment.getEnrollmentDate())
                .status(enrollment.getStatus())
                .current(enrollment.isCurrent())
                .remarks(enrollment.getRemarks())
                .build();
    }
}
