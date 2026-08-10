package com.college.erp.module.academic.dto;

import java.util.UUID;

import com.college.erp.module.academic.domain.Section;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SectionDto {

    private UUID id;
    private UUID batchId;
    private String name;
    private int capacity;
    private boolean active;

    public static SectionDto from(Section section) {
        return SectionDto.builder()
                .id(section.getId())
                .batchId(section.getBatch().getId())
                .name(section.getName())
                .capacity(section.getCapacity())
                .active(section.isActive())
                .build();
    }
}
