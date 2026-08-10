package com.college.erp.module.communication.dto;

import java.time.Instant;
import java.util.UUID;

import com.college.erp.module.communication.domain.HelpdeskTicket;
import com.college.erp.module.communication.domain.TicketCategory;
import com.college.erp.module.communication.domain.TicketPriority;
import com.college.erp.module.communication.domain.TicketStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HelpdeskTicketDto {

    private UUID id;
    private UUID raisedByUserId;
    private String title;
    private String description;
    private TicketCategory category;
    private TicketPriority priority;
    private TicketStatus status;
    private UUID assignedTo;
    private Instant resolvedAt;
    private String resolution;

    public static HelpdeskTicketDto from(HelpdeskTicket ticket) {
        return HelpdeskTicketDto.builder()
                .id(ticket.getId())
                .raisedByUserId(ticket.getRaisedByUserId())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .category(ticket.getCategory())
                .priority(ticket.getPriority())
                .status(ticket.getStatus())
                .assignedTo(ticket.getAssignedTo())
                .resolvedAt(ticket.getResolvedAt())
                .resolution(ticket.getResolution())
                .build();
    }
}
