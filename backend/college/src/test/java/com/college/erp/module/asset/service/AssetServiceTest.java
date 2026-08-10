package com.college.erp.module.asset.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.module.academic.domain.Department;
import com.college.erp.module.academic.service.DepartmentService;
import com.college.erp.module.asset.domain.Asset;
import com.college.erp.module.asset.domain.AssetCategory;
import com.college.erp.module.asset.domain.AssetCategoryRepository;
import com.college.erp.module.asset.domain.AssetRepository;
import com.college.erp.module.asset.domain.AssetStatus;
import com.college.erp.module.asset.dto.AssetDto;
import com.college.erp.module.asset.dto.RegisterAssetRequest;
import com.college.erp.module.finance.service.ProcurementService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** 03-srs.md [AST-001]. Named to match the module's other *ServiceTest files. */
@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    @Mock
    private AssetCategoryRepository assetCategoryRepository;
    @Mock
    private AssetRepository assetRepository;
    @Mock
    private DepartmentService departmentService;
    @Mock
    private ProcurementService procurementService;
    @Mock
    private AuditLogWriter auditLogWriter;

    private final AssetBarcodeService assetBarcodeService = new AssetBarcodeService();
    private AssetService assetService;

    private Department buildDepartment() {
        Department department = Department.builder().name("Computer Science").code("CSE").build();
        department.setId(UUID.randomUUID());
        return department;
    }

    private AssetCategory buildCategory(String name) {
        AssetCategory category = AssetCategory.builder().name(name).depreciationRatePct(10).build();
        category.setId(UUID.randomUUID());
        return category;
    }

    @Test
    @DisplayName("register() — tag is prefixed by the first three letters of the category name, "
            + "zero-padded sequence, matching 03-srs.md [AST-001]'s \"COM-001042\" example")
    void register_generatesTagFromCategoryPrefix() {
        // ARRANGE
        assetService = new AssetService(assetCategoryRepository, assetRepository, departmentService,
                procurementService, assetBarcodeService, auditLogWriter);
        AssetCategory category = buildCategory("Computers");
        Department department = buildDepartment();
        when(assetCategoryRepository.findByIdAndDeletedFalse(category.getId())).thenReturn(Optional.of(category));
        when(departmentService.getDepartmentEntity(department.getId())).thenReturn(department);
        when(assetRepository.countByDeletedFalse()).thenReturn(41L);
        when(assetRepository.existsByAssetTag(any())).thenReturn(false);
        when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> {
            Asset asset = inv.getArgument(0);
            asset.setId(UUID.randomUUID());
            return asset;
        });

        RegisterAssetRequest request = new RegisterAssetRequest();
        request.setName("Dell OptiPlex 7090");
        request.setCategoryId(category.getId());
        request.setDepartmentId(department.getId());
        request.setPurchaseCost(BigDecimal.valueOf(65000));
        request.setPurchaseDate(LocalDate.of(2026, 1, 15));

        // ACT
        AssetDto dto = assetService.register(request);

        // ASSERT
        assertThat(dto.getAssetTag()).isEqualTo("COM-000042");
        assertThat(dto.getStatus()).isEqualTo(AssetStatus.ACTIVE);
        assertThat(dto.getName()).isEqualTo("Dell OptiPlex 7090");
    }

    @Test
    @DisplayName("register() — no category: falls back to the \"GEN\" prefix")
    void register_noCategory_usesGenericPrefix() {
        // ARRANGE
        assetService = new AssetService(assetCategoryRepository, assetRepository, departmentService,
                procurementService, assetBarcodeService, auditLogWriter);
        Department department = buildDepartment();
        when(departmentService.getDepartmentEntity(department.getId())).thenReturn(department);
        when(assetRepository.countByDeletedFalse()).thenReturn(0L);
        when(assetRepository.existsByAssetTag(any())).thenReturn(false);
        when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> {
            Asset asset = inv.getArgument(0);
            asset.setId(UUID.randomUUID());
            return asset;
        });

        RegisterAssetRequest request = new RegisterAssetRequest();
        request.setName("Whiteboard");
        request.setDepartmentId(department.getId());

        // ACT
        AssetDto dto = assetService.register(request);

        // ASSERT
        assertThat(dto.getAssetTag()).isEqualTo("GEN-000001");
    }

    @Test
    @DisplayName("register() — a tag collision advances the sequence until it finds a free one")
    void register_tagCollision_retriesWithNextSequence() {
        // ARRANGE
        assetService = new AssetService(assetCategoryRepository, assetRepository, departmentService,
                procurementService, assetBarcodeService, auditLogWriter);
        Department department = buildDepartment();
        when(departmentService.getDepartmentEntity(department.getId())).thenReturn(department);
        when(assetRepository.countByDeletedFalse()).thenReturn(0L);
        when(assetRepository.existsByAssetTag("GEN-000001")).thenReturn(true);
        when(assetRepository.existsByAssetTag("GEN-000002")).thenReturn(false);
        ArgumentCaptor<Asset> captor = ArgumentCaptor.forClass(Asset.class);
        when(assetRepository.save(captor.capture())).thenAnswer(inv -> {
            Asset asset = inv.getArgument(0);
            asset.setId(UUID.randomUUID());
            return asset;
        });

        RegisterAssetRequest request = new RegisterAssetRequest();
        request.setName("Chair");
        request.setDepartmentId(department.getId());

        // ACT
        assetService.register(request);

        // ASSERT
        assertThat(captor.getValue().getAssetTag()).isEqualTo("GEN-000002");
    }

    @Test
    @DisplayName("getById() — asset not found raises ResourceNotFoundException")
    void getById_notFound_throws() {
        // ARRANGE
        assetService = new AssetService(assetCategoryRepository, assetRepository, departmentService,
                procurementService, assetBarcodeService, auditLogWriter);
        UUID id = UUID.randomUUID();
        when(assetRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.empty());

        // ACT / ASSERT
        assertThatThrownBy(() -> assetService.getById(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getBarcodeImage() — returns a non-empty PNG generated from the asset tag")
    void getBarcodeImage_returnsPngBytes() {
        // ARRANGE
        assetService = new AssetService(assetCategoryRepository, assetRepository, departmentService,
                procurementService, assetBarcodeService, auditLogWriter);
        Asset asset = Asset.builder().assetTag("COM-000042").name("Laptop").build();
        asset.setId(UUID.randomUUID());
        when(assetRepository.findByIdAndDeletedFalse(asset.getId())).thenReturn(Optional.of(asset));

        // ACT
        byte[] png = assetService.getBarcodeImage(asset.getId());

        // ASSERT
        assertThat(png).isNotEmpty();
        // PNG magic bytes
        assertThat(png[0]).isEqualTo((byte) 0x89);
        assertThat(png[1]).isEqualTo((byte) 'P');
    }
}
