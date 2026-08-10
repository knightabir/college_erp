package com.college.erp.module.asset.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.AccessDeniedException;
import com.college.erp.config.ErpProperties;
import com.college.erp.module.academic.domain.Department;
import com.college.erp.module.academic.service.DepartmentService;
import com.college.erp.module.asset.domain.Asset;
import com.college.erp.module.asset.domain.AssetCustodyLog;
import com.college.erp.module.asset.domain.AssetCustodyLogRepository;
import com.college.erp.module.asset.domain.AssetRepository;
import com.college.erp.module.asset.domain.AssetStatus;
import com.college.erp.module.asset.dto.AssetCustodyLogDto;
import com.college.erp.module.asset.dto.AssetDto;
import com.college.erp.module.asset.dto.DisposeAssetRequest;
import com.college.erp.module.asset.dto.TransferAssetRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** 03-srs.md [AST-002]/[AST-004]. Named to match the module's other *ServiceTest files. */
@ExtendWith(MockitoExtension.class)
class AssetTransferServiceTest {

    @Mock
    private AssetRepository assetRepository;
    @Mock
    private AssetCustodyLogRepository assetCustodyLogRepository;
    @Mock
    private AssetService assetService;
    @Mock
    private DepartmentService departmentService;
    @Mock
    private AuditLogWriter auditLogWriter;

    private final ErpProperties erpProperties = new ErpProperties();
    private AssetTransferService assetTransferService;

    private Department buildDepartment(String code) {
        Department department = Department.builder().name(code).code(code).build();
        department.setId(UUID.randomUUID());
        return department;
    }

    private Asset buildAsset(Department department, BigDecimal purchaseCost) {
        Asset asset = Asset.builder()
                .assetTag("COM-000001")
                .name("Projector")
                .department(department)
                .roomLocation("Room 101")
                .assignedToUserId(UUID.randomUUID())
                .status(AssetStatus.ACTIVE)
                .purchaseCost(purchaseCost)
                .build();
        asset.setId(UUID.randomUUID());
        return asset;
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("transfer() — writes an immutable custody row and moves the asset's current dept/location/custodian")
    void transfer_writesCustodyLogAndMovesAsset() {
        // ARRANGE
        assetTransferService = new AssetTransferService(assetRepository, assetCustodyLogRepository, assetService,
                departmentService, erpProperties, auditLogWriter);
        Department fromDept = buildDepartment("CSE");
        Department toDept = buildDepartment("ECE");
        Asset asset = buildAsset(fromDept, BigDecimal.valueOf(10_000));
        UUID toUserId = UUID.randomUUID();
        UUID transferredByUserId = UUID.randomUUID();

        when(assetService.getAssetEntity(asset.getId())).thenReturn(asset);
        when(departmentService.getDepartmentEntity(toDept.getId())).thenReturn(toDept);
        ArgumentCaptor<AssetCustodyLog> logCaptor = ArgumentCaptor.forClass(AssetCustodyLog.class);
        when(assetCustodyLogRepository.save(logCaptor.capture())).thenAnswer(inv -> {
            AssetCustodyLog log = inv.getArgument(0);
            return log.toBuilder().id(UUID.randomUUID()).build();
        });
        when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));

        TransferAssetRequest request = new TransferAssetRequest();
        request.setToDepartmentId(toDept.getId());
        request.setToUserId(toUserId);
        request.setToLocation("Room 305, Block B");
        request.setReason("New faculty assignment");

        // ACT
        AssetCustodyLogDto dto = assetTransferService.transfer(asset.getId(), request, transferredByUserId);

        // ASSERT — the log captures the PRE-transfer department/location as "from"
        AssetCustodyLog captured = logCaptor.getValue();
        assertThat(captured.getFromDepartmentId()).isEqualTo(fromDept.getId());
        assertThat(captured.getToDepartmentId()).isEqualTo(toDept.getId());
        assertThat(captured.getFromLocation()).isEqualTo("Room 101");
        assertThat(captured.getToLocation()).isEqualTo("Room 305, Block B");
        assertThat(captured.getTransferredByUserId()).isEqualTo(transferredByUserId);
        assertThat(dto.getToDepartmentId()).isEqualTo(toDept.getId());
        // ASSERT — the asset itself now reflects the new department/location/custodian
        assertThat(asset.getDepartment()).isEqualTo(toDept);
        assertThat(asset.getRoomLocation()).isEqualTo("Room 305, Block B");
        assertThat(asset.getAssignedToUserId()).isEqualTo(toUserId);
    }

    @Test
    @DisplayName("custodyChain() — returns the chain oldest-first, per 05-api-documentation.md §9")
    void custodyChain_returnsOldestFirst() {
        // ARRANGE
        assetTransferService = new AssetTransferService(assetRepository, assetCustodyLogRepository, assetService,
                departmentService, erpProperties, auditLogWriter);
        UUID assetId = UUID.randomUUID();
        Asset asset = Asset.builder().assetTag("COM-000001").name("Projector").build();
        asset.setId(assetId);
        AssetCustodyLog first = AssetCustodyLog.builder().asset(asset).toDepartmentId(UUID.randomUUID())
                .transferredByUserId(UUID.randomUUID()).transferredAt(java.time.Instant.now()).build();
        when(assetCustodyLogRepository.findByAssetIdOrderByTransferredAtAsc(assetId)).thenReturn(List.of(first));

        // ACT
        List<AssetCustodyLogDto> chain = assetTransferService.custodyChain(assetId);

        // ASSERT
        assertThat(chain).hasSize(1);
    }

    @Test
    @DisplayName("dispose() — below the value threshold, an Asset Manager may dispose directly")
    void dispose_belowThreshold_succeedsWithoutSuperAdmin() {
        // ARRANGE
        assetTransferService = new AssetTransferService(assetRepository, assetCustodyLogRepository, assetService,
                departmentService, erpProperties, auditLogWriter);
        BigDecimal belowThreshold = erpProperties.getAsset().getDisposalValueThreshold().subtract(BigDecimal.ONE);
        Asset asset = buildAsset(buildDepartment("CSE"), belowThreshold);
        when(assetService.getAssetEntity(asset.getId())).thenReturn(asset);
        when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(
                "asset.manager", null, List.of(new SimpleGrantedAuthority("ROLE_ASSET_MANAGER"))));

        DisposeAssetRequest request = new DisposeAssetRequest();
        request.setReason("Beyond economical repair");

        // ACT
        AssetDto dto = assetTransferService.dispose(asset.getId(), request);

        // ASSERT
        assertThat(dto.getStatus()).isEqualTo(AssetStatus.DISPOSED);
    }

    @Test
    @DisplayName("dispose() — at/above the value threshold, an Asset Manager alone is rejected "
            + "(03-srs.md [AST-004]: Admin must confirm)")
    void dispose_atOrAboveThreshold_assetManagerAloneIsRejected() {
        // ARRANGE
        assetTransferService = new AssetTransferService(assetRepository, assetCustodyLogRepository, assetService,
                departmentService, erpProperties, auditLogWriter);
        BigDecimal atThreshold = erpProperties.getAsset().getDisposalValueThreshold();
        Asset asset = buildAsset(buildDepartment("CSE"), atThreshold);
        when(assetService.getAssetEntity(asset.getId())).thenReturn(asset);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(
                "asset.manager", null, List.of(new SimpleGrantedAuthority("ROLE_ASSET_MANAGER"))));

        DisposeAssetRequest request = new DisposeAssetRequest();
        request.setReason("Beyond economical repair");

        // ACT / ASSERT
        assertThatThrownBy(() -> assetTransferService.dispose(asset.getId(), request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("dispose() — at/above the value threshold, a Super Admin succeeds")
    void dispose_atOrAboveThreshold_superAdminSucceeds() {
        // ARRANGE
        assetTransferService = new AssetTransferService(assetRepository, assetCustodyLogRepository, assetService,
                departmentService, erpProperties, auditLogWriter);
        BigDecimal aboveThreshold = erpProperties.getAsset().getDisposalValueThreshold().add(BigDecimal.ONE);
        Asset asset = buildAsset(buildDepartment("CSE"), aboveThreshold);
        when(assetService.getAssetEntity(asset.getId())).thenReturn(asset);
        when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(
                "super.admin", null, List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))));

        DisposeAssetRequest request = new DisposeAssetRequest();
        request.setReason("Beyond economical repair");

        // ACT / ASSERT — no exception
        assertThatCode(() -> assetTransferService.dispose(asset.getId(), request)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("dispose() — no purchase cost on record is treated as below threshold")
    void dispose_noPurchaseCost_treatedAsBelowThreshold() {
        // ARRANGE
        assetTransferService = new AssetTransferService(assetRepository, assetCustodyLogRepository, assetService,
                departmentService, erpProperties, auditLogWriter);
        Asset asset = buildAsset(buildDepartment("CSE"), null);
        when(assetService.getAssetEntity(asset.getId())).thenReturn(asset);
        when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));

        DisposeAssetRequest request = new DisposeAssetRequest();
        request.setReason("Lost beyond recovery");

        // ACT / ASSERT — no exception, and no authentication needed at all
        assertThatCode(() -> assetTransferService.dispose(asset.getId(), request)).doesNotThrowAnyException();
    }
}
