package com.college.erp.module.academic.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.college.erp.common.dto.ApiResponse;
import com.college.erp.module.academic.dto.SectionDto;
import com.college.erp.module.academic.service.SectionService;

import lombok.RequiredArgsConstructor;

/** Create/list is on BatchController (nested resource, see 05-api-documentation.md §3). */
@RestController
@RequestMapping("/api/v1/academic/sections")
@RequiredArgsConstructor
public class SectionController {

    private final SectionService sectionService;

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SectionDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(sectionService.getById(id)));
    }
}
