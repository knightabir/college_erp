package com.college.erp.module.communication.dto;

import java.time.Instant;
import java.util.UUID;

import com.college.erp.module.communication.domain.Notice;
import com.college.erp.module.communication.domain.NoticeTargetType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NoticeDto {

    private UUID id;
    private String title;
    private String body;
    private NoticeTargetType targetType;
    private UUID targetId;
    private boolean pinned;
    private Instant publishedAt;
    private Instant expiresAt;

    public static NoticeDto from(Notice notice) {
        return NoticeDto.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .body(notice.getBody())
                .targetType(notice.getTargetType())
                .targetId(notice.getTargetId())
                .pinned(notice.isPinned())
                .publishedAt(notice.getPublishedAt())
                .expiresAt(notice.getExpiresAt())
                .build();
    }
}
