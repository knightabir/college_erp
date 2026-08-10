package com.college.erp.module.finance.dto;

import java.util.UUID;

import com.college.erp.module.finance.domain.AccountHead;
import com.college.erp.module.finance.domain.AccountType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AccountHeadDto {

    private UUID id;
    private String code;
    private String name;
    private String description;
    private AccountType type;
    private UUID parentId;
    private boolean active;

    public static AccountHeadDto from(AccountHead head) {
        return AccountHeadDto.builder()
                .id(head.getId())
                .code(head.getCode())
                .name(head.getName())
                .description(head.getDescription())
                .type(head.getType())
                .parentId(head.getParent() == null ? null : head.getParent().getId())
                .active(head.isActive())
                .build();
    }
}
