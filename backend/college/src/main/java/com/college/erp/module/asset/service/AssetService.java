package com.college.erp.module.asset.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.module.academic.domain.Department;
import com.college.erp.module.academic.service.DepartmentService;
import com.college.erp.module.asset.domain.Asset;
import com.college.erp.module.asset.domain.AssetCategory;
import com.college.erp.module.asset.domain.AssetCategoryRepository;
import com.college.erp.module.asset.domain.AssetRepository;
import com.college.erp.module.asset.domain.AssetStatus;
import com.college.erp.module.asset.dto.AssetCategoryDto;
import com.college.erp.module.asset.dto.AssetDto;
import com.college.erp.module.asset.dto.CreateAssetCategoryRequest;
import com.college.erp.module.asset.dto.RegisterAssetRequest;
import com.college.erp.module.finance.domain.Vendor;
import com.college.erp.module.finance.service.ProcurementService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 03-srs.md [AST-001], 05-api-documentation.md §9 "Asset Management". Categories, registration
 * (auto-generated tag) and read access. Transfer/disposal is AssetTransferService;
 * maintenance tickets are MaintenanceService — split the same way library separates
 * CatalogService from CirculationService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetService {

    private static final String MODULE = "ASSET";
    private static final String DEFAULT_PREFIX = "GEN";

    private final AssetCategoryRepository assetCategoryRepository;
    private final AssetRepository assetRepository;
    private final DepartmentService departmentService;
    private final ProcurementService procurementService;
    private final AssetBarcodeService assetBarcodeService;
    private final AuditLogWriter auditLogWriter;

    @Transactional
    public AssetCategoryDto createCategory(CreateAssetCategoryRequest request) {
        AssetCategory category = AssetCategory.builder()
                .name(request.getName())
                .depreciationRatePct(request.getDepreciationRatePct())
                .build();
        AssetCategory saved = assetCategoryRepository.save(category);

        auditLogWriter.write(AuditAction.CREATE, MODULE, "AssetCategory", saved.getId().toString(),
                "Created category: " + saved.getName());

        return AssetCategoryDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<AssetCategoryDto> listCategories() {
        return assetCategoryRepository.findByDeletedFalse().stream()
                .map(AssetCategoryDto::from)
                .toList();
    }

    /** 03-srs.md [AST-001]: tag auto-generated, prefixed by category, e.g. "COM-001042". */
    @Transactional
    public AssetDto register(RegisterAssetRequest request) {
        AssetCategory category = request.getCategoryId() != null
                ? getCategoryEntity(request.getCategoryId())
                : null;
        Department department = departmentService.getDepartmentEntity(request.getDepartmentId());
        Vendor vendor = request.getVendorId() != null
                ? procurementService.getVendorEntity(request.getVendorId())
                : null;

        Asset asset = Asset.builder()
                .assetTag(generateAssetTag(category))
                .name(request.getName())
                .description(request.getDescription())
                .category(category)
                .department(department)
                .roomLocation(request.getRoomLocation())
                .purchaseDate(request.getPurchaseDate())
                .purchaseCost(request.getPurchaseCost())
                .vendor(vendor)
                .warrantyExpiry(request.getWarrantyExpiry())
                .assignedToUserId(request.getAssignedToUserId())
                .status(AssetStatus.ACTIVE)
                .build();

        Asset saved = assetRepository.save(asset);
        auditLogWriter.write(AuditAction.CREATE, MODULE, "Asset", saved.getId().toString(),
                "Registered asset " + saved.getAssetTag() + ": " + saved.getName());
        log.info("Asset registered: [{}] {}", saved.getAssetTag(), saved.getName());

        return AssetDto.from(saved);
    }

    @Transactional(readOnly = true)
    public AssetDto getById(UUID id) {
        return AssetDto.from(getEntity(id));
    }

    @Transactional(readOnly = true)
    public AssetDto getByTag(String assetTag) {
        return AssetDto.from(assetRepository.findByAssetTagAndDeletedFalse(assetTag)
                .orElseThrow(() -> new ResourceNotFoundException("Asset with tag", assetTag)));
    }

    @Transactional(readOnly = true)
    public Page<AssetDto> listByDepartment(UUID departmentId, Pageable pageable) {
        return assetRepository.findByDepartmentIdAndDeletedFalse(departmentId, pageable).map(AssetDto::from);
    }

    /** 05-api-documentation.md §9: GET /assets/{id}/barcode-image — generated on demand, not
     *  persisted, matching library's BarcodeService/CatalogService.getBarcodeImage pattern. */
    @Transactional(readOnly = true)
    public byte[] getBarcodeImage(UUID id) {
        Asset asset = getEntity(id);
        return assetBarcodeService.generateCode128Png(asset.getAssetTag());
    }

    /** For AssetTransferService/MaintenanceService — see BatchService.getBatchEntity javadoc. */
    @Transactional(readOnly = true)
    public Asset getAssetEntity(UUID id) {
        return getEntity(id);
    }

    private AssetCategory getCategoryEntity(UUID id) {
        return assetCategoryRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("AssetCategory", id));
    }

    private Asset getEntity(UUID id) {
        return assetRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset", id));
    }

    private String generateAssetTag(AssetCategory category) {
        String prefix = category != null && category.getName() != null && category.getName().length() >= 3
                ? category.getName().substring(0, 3).toUpperCase()
                : DEFAULT_PREFIX;
        long sequence = assetRepository.countByDeletedFalse() + 1;
        String tag = prefix + "-" + String.format("%06d", sequence);
        // Extremely unlikely with a monotonic counter, but avoid a UNIQUE violation on retry.
        while (assetRepository.existsByAssetTag(tag)) {
            sequence++;
            tag = prefix + "-" + String.format("%06d", sequence);
        }
        return tag;
    }
}
