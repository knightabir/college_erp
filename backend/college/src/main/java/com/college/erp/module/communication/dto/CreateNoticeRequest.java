package com.college.erp.module.communication.dto;

import java.time.Instant;
import java.util.UUID;

import com.college.erp.module.communication.domain.NoticeTargetType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** 05-api-documentation.md §11: POST /notices. */
@Getter
@Setter
public class CreateNoticeRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String body;

    @NotNull
    private NoticeTargetType targetType;

    private UUID targetId;

    private boolean pinned;

    private Instant expiresAt;
}
