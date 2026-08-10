package com.college.erp.module.asset.service;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.module.asset.domain.Asset;
import com.college.erp.module.asset.domain.MaintenanceTicket;
import com.college.erp.module.asset.domain.MaintenanceTicketRepository;
import com.college.erp.module.asset.domain.TicketPriority;
import com.college.erp.module.asset.domain.TicketStatus;
import com.college.erp.module.asset.dto.MaintenanceTicketDto;
import com.college.erp.module.asset.dto.RaiseTicketRequest;
import com.college.erp.module.asset.dto.ResolveTicketRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** 03-srs.md [AST-003]. Named to match the module's other *ServiceTest files. */
@ExtendWith(MockitoExtension.class)
class MaintenanceServiceTest {

    @Mock
    private MaintenanceTicketRepository maintenanceTicketRepository;
    @Mock
    private AssetService assetService;
    @Mock
    private AuditLogWriter auditLogWriter;

    private MaintenanceService maintenanceService;

    private Asset buildAsset() {
        Asset asset = Asset.builder().assetTag("COM-000001").name("Projector").build();
        asset.setId(UUID.randomUUID());
        return asset;
    }

    @Test
    @DisplayName("raise() — any authenticated user may raise a ticket; defaults priority to MEDIUM")
    void raise_defaultsPriorityToMedium() {
        // ARRANGE
        maintenanceService = new MaintenanceService(maintenanceTicketRepository, assetService, auditLogWriter);
        Asset asset = buildAsset();
        when(assetService.getAssetEntity(asset.getId())).thenReturn(asset);
        when(maintenanceTicketRepository.save(any(MaintenanceTicket.class))).thenAnswer(inv -> {
            MaintenanceTicket ticket = inv.getArgument(0);
            ticket.setId(UUID.randomUUID());
            return ticket;
        });

        RaiseTicketRequest request = new RaiseTicketRequest();
        request.setTitle("Projector not displaying correctly");
        request.setDescription("Image is distorted on the left side");

        // ACT
        MaintenanceTicketDto dto = maintenanceService.raise(asset.getId(), request, UUID.randomUUID());

        // ASSERT
        assertThat(dto.getPriority()).isEqualTo(TicketPriority.MEDIUM);
        assertThat(dto.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(dto.getAssetTag()).isEqualTo("COM-000001");
    }

    @Test
    @DisplayName("raise() — an explicit priority is honored")
    void raise_explicitPriority_isHonored() {
        // ARRANGE
        maintenanceService = new MaintenanceService(maintenanceTicketRepository, assetService, auditLogWriter);
        Asset asset = buildAsset();
        when(assetService.getAssetEntity(asset.getId())).thenReturn(asset);
        when(maintenanceTicketRepository.save(any(MaintenanceTicket.class))).thenAnswer(inv -> {
            MaintenanceTicket ticket = inv.getArgument(0);
            ticket.setId(UUID.randomUUID());
            return ticket;
        });

        RaiseTicketRequest request = new RaiseTicketRequest();
        request.setTitle("Server room AC failure");
        request.setPriority(TicketPriority.CRITICAL);

        // ACT
        MaintenanceTicketDto dto = maintenanceService.raise(asset.getId(), request, UUID.randomUUID());

        // ASSERT
        assertThat(dto.getPriority()).isEqualTo(TicketPriority.CRITICAL);
    }

    @Test
    @DisplayName("resolve() — sets status RESOLVED, resolution text, resolvedAt, and assigns "
            + "the resolver if nobody was assigned yet")
    void resolve_setsResolutionFields() {
        // ARRANGE
        maintenanceService = new MaintenanceService(maintenanceTicketRepository, assetService, auditLogWriter);
        Asset asset = buildAsset();
        MaintenanceTicket ticket = MaintenanceTicket.builder()
                .asset(asset).reportedBy(UUID.randomUUID()).title("HDMI cable damaged")
                .priority(TicketPriority.HIGH).status(TicketStatus.OPEN).build();
        ticket.setId(UUID.randomUUID());
        UUID resolverId = UUID.randomUUID();
        when(maintenanceTicketRepository.findByIdAndDeletedFalse(ticket.getId())).thenReturn(Optional.of(ticket));
        when(maintenanceTicketRepository.save(any(MaintenanceTicket.class))).thenAnswer(inv -> inv.getArgument(0));

        ResolveTicketRequest request = new ResolveTicketRequest();
        request.setResolution("Replaced the HDMI cable. Working correctly now.");

        // ACT
        MaintenanceTicketDto dto = maintenanceService.resolve(ticket.getId(), request, resolverId);

        // ASSERT
        assertThat(dto.getStatus()).isEqualTo(TicketStatus.RESOLVED);
        assertThat(dto.getResolution()).isEqualTo("Replaced the HDMI cable. Working correctly now.");
        assertThat(dto.getResolvedAt()).isNotNull();
        assertThat(dto.getAssignedTo()).isEqualTo(resolverId);
    }

    @Test
    @DisplayName("resolve() — unknown ticket id raises ResourceNotFoundException")
    void resolve_unknownTicket_throws() {
        // ARRANGE
        maintenanceService = new MaintenanceService(maintenanceTicketRepository, assetService, auditLogWriter);
        UUID ticketId = UUID.randomUUID();
        when(maintenanceTicketRepository.findByIdAndDeletedFalse(ticketId)).thenReturn(Optional.empty());
        ResolveTicketRequest request = new ResolveTicketRequest();
        request.setResolution("Fixed");

        // ACT / ASSERT
        assertThatThrownBy(() -> maintenanceService.resolve(ticketId, request, UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
