package com.college.erp.module.academic.dto;

import java.util.UUID;

import com.college.erp.module.academic.domain.Course;
import com.college.erp.module.academic.domain.CourseLevel;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CourseDto {

    private UUID id;
    private UUID departmentId;
    private String departmentName;
    private String name;
    private String code;
    private CourseLevel level;
    private int durationYears;
    private int totalCredits;
    private int intakeCapacity;
    private String description;
    private boolean active;

    public static CourseDto from(Course course) {
        return CourseDto.builder()
                .id(course.getId())
                .departmentId(course.getDepartment().getId())
                .departmentName(course.getDepartment().getName())
                .name(course.getName())
                .code(course.getCode())
                .level(course.getLevel())
                .durationYears(course.getDurationYears())
                .totalCredits(course.getTotalCredits())
                .intakeCapacity(course.getIntakeCapacity())
                .description(course.getDescription())
                .active(course.isActive())
                .build();
    }
}
