package com.college.erp.module.asset.dto;

import java.time.Instant;
import java.util.UUID;

import com.college.erp.module.asset.domain.MaintenanceTicket;
import com.college.erp.module.asset.domain.TicketPriority;
import com.college.erp.module.asset.domain.TicketStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MaintenanceTicketDto {

    private UUID id;
    private UUID assetId;
    private String assetTag;
    private UUID reportedBy;
    private String title;
    private String description;
    private TicketPriority priority;
    private TicketStatus status;
    private UUID assignedTo;
    private Instant resolvedAt;
    private String resolution;

    public static MaintenanceTicketDto from(MaintenanceTicket ticket) {
        return MaintenanceTicketDto.builder()
                .id(ticket.getId())
                .assetId(ticket.getAsset().getId())
                .assetTag(ticket.getAsset().getAssetTag())
                .reportedBy(ticket.getReportedBy())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .priority(ticket.getPriority())
                .status(ticket.getStatus())
                .assignedTo(ticket.getAssignedTo())
                .resolvedAt(ticket.getResolvedAt())
                .resolution(ticket.getResolution())
                .build();
    }
}
