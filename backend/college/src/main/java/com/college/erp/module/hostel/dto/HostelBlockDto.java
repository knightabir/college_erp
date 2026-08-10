package com.college.erp.module.hostel.dto;

import java.util.UUID;

import com.college.erp.module.hostel.domain.HostelBlock;
import com.college.erp.module.hostel.domain.HostelGender;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HostelBlockDto {

    private UUID id;
    private String name;
    private HostelGender gender;
    private UUID wardenId;
    private Integer capacity;

    public static HostelBlockDto from(HostelBlock block) {
        return HostelBlockDto.builder()
                .id(block.getId())
                .name(block.getName())
                .gender(block.getGender())
                .wardenId(block.getWardenId())
                .capacity(block.getCapacity())
                .build();
    }
}
