package com.college.erp.module.library.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.college.erp.common.dto.ApiResponse;
import com.college.erp.common.dto.PageResponse;
import com.college.erp.module.library.dto.BookCopyDto;
import com.college.erp.module.library.dto.BookTitleDto;
import com.college.erp.module.library.dto.CreateBookCopyRequest;
import com.college.erp.module.library.dto.CreateBookTitleRequest;
import com.college.erp.module.library.service.CatalogService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** 05-api-documentation.md §8 "Catalog". */
@RestController
@RequestMapping("/api/v1/library/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping("/titles")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<BookTitleDto>>> searchTitles(
            @RequestParam(required = false) String q, Pageable pageable) {
        Page<BookTitleDto> page = catalogService.search(q, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(page)));
    }

    @GetMapping("/titles/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BookTitleDto>> getTitle(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(catalogService.getTitle(id)));
    }

    @PostMapping("/titles")
    @PreAuthorize("hasRole('CHIEF_LIBRARIAN')")
    public ResponseEntity<ApiResponse<BookTitleDto>> createTitle(
            @Valid @RequestBody CreateBookTitleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(catalogService.createTitle(request)));
    }

    @PostMapping("/titles/{titleId}/copies")
    @PreAuthorize("hasAnyRole('CHIEF_LIBRARIAN','LIBRARY_ASSISTANT')")
    public ResponseEntity<ApiResponse<BookCopyDto>> addCopy(
            @PathVariable UUID titleId, @Valid @RequestBody CreateBookCopyRequest request) {
        BookCopyDto copy = catalogService.addCopy(titleId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Copy added — barcode: " + copy.getBarcode(), copy));
    }

    @GetMapping("/titles/{titleId}/copies")
    @PreAuthorize("hasAnyRole('CHIEF_LIBRARIAN','LIBRARY_ASSISTANT')")
    public ResponseEntity<ApiResponse<List<BookCopyDto>>> listCopies(@PathVariable UUID titleId) {
        return ResponseEntity.ok(ApiResponse.success(catalogService.listCopies(titleId)));
    }

    @GetMapping("/copies/barcode/{barcode}")
    @PreAuthorize("hasAnyRole('CHIEF_LIBRARIAN','LIBRARY_ASSISTANT')")
    public ResponseEntity<ApiResponse<BookCopyDto>> getByBarcode(@PathVariable String barcode) {
        return ResponseEntity.ok(ApiResponse.success(catalogService.getByBarcode(barcode)));
    }

    @GetMapping(value = "/copies/{copyId}/barcode-image", produces = MediaType.IMAGE_PNG_VALUE)
    @PreAuthorize("hasAnyRole('CHIEF_LIBRARIAN','LIBRARY_ASSISTANT')")
    public ResponseEntity<byte[]> getBarcodeImage(@PathVariable UUID copyId) {
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(catalogService.getBarcodeImage(copyId));
    }

    @PatchMapping("/copies/{copyId}/mark-lost")
    @PreAuthorize("hasRole('CHIEF_LIBRARIAN')")
    public ResponseEntity<ApiResponse<Void>> markLost(@PathVariable UUID copyId) {
        catalogService.markLost(copyId);
        return ResponseEntity.ok(ApiResponse.success("Copy marked lost", null));
    }
}
