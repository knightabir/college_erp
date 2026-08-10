package com.college.erp.module.academic.dto;

import java.util.UUID;

import com.college.erp.module.academic.domain.Subject;
import com.college.erp.module.academic.domain.SubjectType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SubjectDto {

    private UUID id;
    private String code;
    private String name;
    private UUID departmentId;
    private int credits;
    private SubjectType type;
    private String description;
    private boolean active;

    public static SubjectDto from(Subject subject) {
        return SubjectDto.builder()
                .id(subject.getId())
                .code(subject.getCode())
                .name(subject.getName())
                .departmentId(subject.getDepartment().getId())
                .credits(subject.getCredits())
                .type(subject.getType())
                .description(subject.getDescription())
                .active(subject.isActive())
                .build();
    }
}
