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
import com.college.erp.module.examination.dto.ResultDto;
import com.college.erp.module.examination.service.GradeCardService;
import com.college.erp.module.examination.service.ResultProcessingService;

import lombok.RequiredArgsConstructor;

/** 05-api-documentation.md §6 POST /exams/{examId}/results/publish, GET .../results/student/.../term/... */
@RestController
@RequestMapping("/api/v1/exams")
@RequiredArgsConstructor
public class ResultController {

    private final ResultProcessingService resultProcessingService;
    private final GradeCardService gradeCardService;

    @PostMapping("/{examId}/results/publish")
    @PreAuthorize("hasAnyRole('EXAM_CELL','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Integer>> publish(@PathVariable UUID examId) {
        return ResponseEntity.ok(ApiResponse.success(resultProcessingService.publish(examId)));
    }

    @GetMapping("/results/student/{studentId}/term/{termId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HOD','EXAM_CELL') "
            + "or @studentSecurityService.isOwnProfile(#studentId, principal)")
    public ResponseEntity<ApiResponse<ResultDto>> getResult(
            @PathVariable UUID studentId, @PathVariable UUID termId) {
        return ResponseEntity.ok(ApiResponse.success(resultProcessingService.getResult(studentId, termId)));
    }

    @GetMapping("/grade-card/student/{studentId}/term/{termId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HOD','EXAM_CELL') "
            + "or @studentSecurityService.isOwnProfile(#studentId, principal)")
    public ResponseEntity<ApiResponse<ResultDto>> getGradeCard(
            @PathVariable UUID studentId, @PathVariable UUID termId) {
        return ResponseEntity.ok(ApiResponse.success(gradeCardService.getGradeCardData(studentId, termId)));
    }
}
