package com.college.erp.module.admissions.dto;

import java.time.Instant;
import java.util.UUID;

import com.college.erp.module.admissions.domain.MeritList;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MeritListDto {

    private UUID id;
    private UUID courseId;
    private String courseName;
    private UUID academicYearId;
    private String name;
    private Instant publishedAt;

    public static MeritListDto from(MeritList meritList) {
        return MeritListDto.builder()
                .id(meritList.getId())
                .courseId(meritList.getCourse().getId())
                .courseName(meritList.getCourse().getName())
                .academicYearId(meritList.getAcademicYear().getId())
                .name(meritList.getName())
                .publishedAt(meritList.getPublishedAt())
                .build();
    }
}
