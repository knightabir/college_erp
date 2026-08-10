package com.college.erp.module.examination.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.college.erp.common.dto.ApiResponse;
import com.college.erp.module.examination.dto.HallTicketDto;
import com.college.erp.module.examination.dto.HallTicketGenerationResultDto;
import com.college.erp.module.examination.service.HallTicketService;

import lombok.RequiredArgsConstructor;

/** 05-api-documentation.md §6. */
@RestController
@RequestMapping("/api/v1/exams/{examId}/hall-tickets")
@RequiredArgsConstructor
public class HallTicketController {

    private final HallTicketService hallTicketService;

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','EXAM_CELL')")
    public ResponseEntity<ApiResponse<HallTicketGenerationResultDto>> generate(@PathVariable UUID examId) {
        return ResponseEntity.ok(ApiResponse.success(hallTicketService.generate(examId)));
    }

    @GetMapping("/{studentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','EXAM_CELL') "
            + "or @studentSecurityService.isOwnProfile(#studentId, principal)")
    public ResponseEntity<ApiResponse<HallTicketDto>> getForStudent(
            @PathVariable UUID examId, @PathVariable UUID studentId) {
        return ResponseEntity.ok(ApiResponse.success(hallTicketService.getForStudent(examId, studentId)));
    }
}
