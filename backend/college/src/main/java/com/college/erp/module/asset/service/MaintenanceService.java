package com.college.erp.module.asset.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.module.asset.domain.Asset;
import com.college.erp.module.asset.domain.MaintenanceTicket;
import com.college.erp.module.asset.domain.MaintenanceTicketRepository;
import com.college.erp.module.asset.domain.TicketPriority;
import com.college.erp.module.asset.dto.MaintenanceTicketDto;
import com.college.erp.module.asset.dto.RaiseTicketRequest;
import com.college.erp.module.asset.dto.ResolveTicketRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 03-srs.md [AST-003]: any authenticated user may raise a ticket; resolution is restricted to
 * ASSET_MANAGER/MAINTENANCE_STAFF (enforced in MaintenanceController's @PreAuthorize). Only the
 * two documented transitions (OPEN → RESOLVED) are implemented — see TicketStatus javadoc.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaintenanceService {

    private static final String MODULE = "ASSET";

    private final MaintenanceTicketRepository maintenanceTicketRepository;
    private final AssetService assetService;
    private final AuditLogWriter auditLogWriter;

    @Transactional
    public MaintenanceTicketDto raise(UUID assetId, RaiseTicketRequest request, UUID reportedByUserId) {
        Asset asset = assetService.getAssetEntity(assetId);

        MaintenanceTicket ticket = MaintenanceTicket.builder()
                .asset(asset)
                .reportedBy(reportedByUserId)
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(request.getPriority() != null ? request.getPriority() : TicketPriority.MEDIUM)
                .build();
        MaintenanceTicket saved = maintenanceTicketRepository.save(ticket);

        auditLogWriter.write(AuditAction.CREATE, MODULE, "MaintenanceTicket", saved.getId().toString(),
                "Raised ticket for " + asset.getAssetTag() + ": " + saved.getTitle());
        log.info("Maintenance ticket raised for [{}]: {}", asset.getAssetTag(), saved.getTitle());

        return MaintenanceTicketDto.from(saved);
    }

    @Transactional
    public MaintenanceTicketDto resolve(UUID ticketId, ResolveTicketRequest request, UUID resolvedByUserId) {
        MaintenanceTicket ticket = getEntity(ticketId);
        ticket.resolve(resolvedByUserId, request.getResolution(), Instant.now());
        MaintenanceTicket saved = maintenanceTicketRepository.save(ticket);

        auditLogWriter.write(AuditAction.UPDATE, MODULE, "MaintenanceTicket", saved.getId().toString(),
                "Resolved ticket for " + saved.getAsset().getAssetTag());

        return MaintenanceTicketDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<MaintenanceTicketDto> listByAsset(UUID assetId) {
        return maintenanceTicketRepository.findByAssetIdAndDeletedFalse(assetId).stream()
                .map(MaintenanceTicketDto::from)
                .toList();
    }

    private MaintenanceTicket getEntity(UUID id) {
        return maintenanceTicketRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("MaintenanceTicket", id));
    }
}
