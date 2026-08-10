package com.college.erp.module.hr.dto;

import java.util.UUID;

import com.college.erp.module.hr.domain.Designation;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DesignationDto {

    private UUID id;
    private String name;
    private String grade;

    public static DesignationDto from(Designation designation) {
        return DesignationDto.builder()
                .id(designation.getId())
                .name(designation.getName())
                .grade(designation.getGrade())
                .build();
    }
}
