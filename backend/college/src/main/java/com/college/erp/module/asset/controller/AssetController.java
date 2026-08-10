package com.college.erp.module.asset.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.college.erp.common.dto.ApiResponse;
import com.college.erp.common.dto.PageResponse;
import com.college.erp.module.asset.dto.AssetCategoryDto;
import com.college.erp.module.asset.dto.AssetCustodyLogDto;
import com.college.erp.module.asset.dto.AssetDto;
import com.college.erp.module.asset.dto.CreateAssetCategoryRequest;
import com.college.erp.module.asset.dto.DisposeAssetRequest;
import com.college.erp.module.asset.dto.RegisterAssetRequest;
import com.college.erp.module.asset.dto.TransferAssetRequest;
import com.college.erp.module.asset.service.AssetService;
import com.college.erp.module.asset.service.AssetTransferService;
import com.college.erp.security.jwt.UserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** 05-api-documentation.md §9 "Asset Management". */
@RestController
@RequestMapping("/api/v1/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;
    private final AssetTransferService assetTransferService;

    @PostMapping("/categories")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ASSET_MANAGER')")
    public ResponseEntity<ApiResponse<AssetCategoryDto>> createCategory(
            @Valid @RequestBody CreateAssetCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(assetService.createCategory(request)));
    }

    @GetMapping("/categories")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<AssetCategoryDto>>> listCategories() {
        return ResponseEntity.ok(ApiResponse.success(assetService.listCategories()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ASSET_MANAGER')")
    public ResponseEntity<ApiResponse<AssetDto>> register(@Valid @RequestBody RegisterAssetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Asset registered", assetService.register(request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HOD','ASSET_MANAGER','MAINTENANCE_STAFF')")
    public ResponseEntity<ApiResponse<AssetDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(assetService.getById(id)));
    }

    @GetMapping("/tag/{tag}")
    @PreAuthorize("hasAnyRole('ASSET_MANAGER','MAINTENANCE_STAFF')")
    public ResponseEntity<ApiResponse<AssetDto>> getByTag(@PathVariable String tag) {
        return ResponseEntity.ok(ApiResponse.success(assetService.getByTag(tag)));
    }

    @GetMapping("/by-department/{departmentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HOD','ASSET_MANAGER')")
    public ResponseEntity<ApiResponse<PageResponse<AssetDto>>> listByDepartment(
            @PathVariable UUID departmentId, Pageable pageable) {
        Page<AssetDto> page = assetService.listByDepartment(departmentId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(page)));
    }

    @GetMapping(value = "/{id}/barcode-image", produces = MediaType.IMAGE_PNG_VALUE)
    @PreAuthorize("hasRole('ASSET_MANAGER')")
    public ResponseEntity<byte[]> getBarcodeImage(@PathVariable UUID id) {
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(assetService.getBarcodeImage(id));
    }

    @PostMapping("/{id}/transfer")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ASSET_MANAGER')")
    public ResponseEntity<ApiResponse<AssetCustodyLogDto>> transfer(
            @PathVariable UUID id, @Valid @RequestBody TransferAssetRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(assetTransferService.transfer(id, request, principal.getId())));
    }

    @GetMapping("/{id}/custody-chain")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','ASSET_MANAGER')")
    public ResponseEntity<ApiResponse<List<AssetCustodyLogDto>>> custodyChain(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(assetTransferService.custodyChain(id)));
    }

    @PatchMapping("/{id}/dispose")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ASSET_MANAGER')")
    public ResponseEntity<ApiResponse<AssetDto>> dispose(
            @PathVariable UUID id, @Valid @RequestBody DisposeAssetRequest request) {
        return ResponseEntity.ok(ApiResponse.success(assetTransferService.dispose(id, request)));
    }
}
