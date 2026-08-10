package com.college.erp.module.examination.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.college.erp.common.dto.ApiResponse;
import com.college.erp.module.examination.dto.CreateExamRequest;
import com.college.erp.module.examination.dto.ExamDto;
import com.college.erp.module.examination.service.ExamService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** 05-api-documentation.md §6 POST /exams. */
@RestController
@RequestMapping("/api/v1/exams")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','EXAM_CELL')")
    public ResponseEntity<ApiResponse<ExamDto>> schedule(@Valid @RequestBody CreateExamRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Exam scheduled", examService.schedule(request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ExamDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(examService.getById(id)));
    }

    @GetMapping("/by-batch/{batchId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ExamDto>>> listByBatch(@PathVariable UUID batchId) {
        return ResponseEntity.ok(ApiResponse.success(examService.listByBatch(batchId)));
    }
}
