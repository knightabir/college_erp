package com.college.erp.module.communication.dto;

import java.time.Instant;
import java.util.UUID;

import com.college.erp.module.communication.domain.TicketComment;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TicketCommentDto {

    private UUID id;
    private UUID ticketId;
    private UUID userId;
    private String comment;
    private Instant createdAt;

    public static TicketCommentDto from(TicketComment comment) {
        return TicketCommentDto.builder()
                .id(comment.getId())
                .ticketId(comment.getTicket().getId())
                .userId(comment.getUserId())
                .comment(comment.getComment())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
