package com.college.erp.module.examination.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.module.examination.dto.ResultDto;

import lombok.RequiredArgsConstructor;

/**
 * 03-srs.md [EXAM-006] calls for a PDF grade card. No PDF library is in pom.xml and
 * module.storage (needed to persist the generated file) doesn't exist yet — see
 * ResultDto/GradeCardDto discussion in CLAUDE.md. This returns the exact data a grade card needs
 * (ResultProcessingService.getResult already matches the shape EXAM-006 lists: student details via
 * studentId, subject-wise marks, grades, SGPA, CGPA, result status) so the frontend can render one
 * client-side, or so PDF rendering can be added here later without touching the data layer.
 */
@Service
@RequiredArgsConstructor
public class GradeCardService {

    private final ResultProcessingService resultProcessingService;

    @Transactional(readOnly = true)
    public ResultDto getGradeCardData(UUID studentId, UUID termId) {
        return resultProcessingService.getResult(studentId, termId);
    }
}
