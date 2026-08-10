package com.college.erp.module.finance.dto;

import java.util.UUID;

import com.college.erp.module.finance.domain.FeeHead;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FeeHeadDto {

    private UUID id;
    private String code;
    private String name;
    private String description;
    private boolean active;

    public static FeeHeadDto from(FeeHead feeHead) {
        return FeeHeadDto.builder()
                .id(feeHead.getId())
                .code(feeHead.getCode())
                .name(feeHead.getName())
                .description(feeHead.getDescription())
                .active(feeHead.isActive())
                .build();
    }
}
