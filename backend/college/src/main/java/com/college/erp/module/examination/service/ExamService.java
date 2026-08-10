package com.college.erp.module.examination.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.module.academic.domain.Batch;
import com.college.erp.module.academic.domain.Term;
import com.college.erp.module.academic.service.BatchService;
import com.college.erp.module.academic.service.TermService;
import com.college.erp.module.examination.domain.Exam;
import com.college.erp.module.examination.domain.ExamRepository;
import com.college.erp.module.examination.dto.CreateExamRequest;
import com.college.erp.module.examination.dto.ExamDto;

import lombok.RequiredArgsConstructor;

/** 03-srs.md [EXAM-001], 05-api-documentation.md §6 POST /exams. */
@Service
@RequiredArgsConstructor
public class ExamService {

    private static final String MODULE = "EXAMINATION";

    private final ExamRepository examRepository;
    private final BatchService batchService;
    private final TermService termService;
    private final AuditLogWriter auditLogWriter;

    @Transactional
    public ExamDto schedule(CreateExamRequest request) {
        Batch batch = batchService.getBatchEntity(request.getBatchId());
        Term term = termService.getTermEntity(request.getTermId());

        Exam exam = Exam.builder()
                .name(request.getName())
                .examType(request.getExamType())
                .batch(batch)
                .term(term)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();

        Exam saved = examRepository.save(exam);
        auditLogWriter.write(AuditAction.CREATE, MODULE, "Exam", saved.getId().toString(),
                "Scheduled: " + saved.getName());

        return ExamDto.from(saved);
    }

    @Transactional(readOnly = true)
    public ExamDto getById(UUID id) {
        return ExamDto.from(getEntity(id));
    }

    @Transactional(readOnly = true)
    public List<ExamDto> listByBatch(UUID batchId) {
        return examRepository.findByBatchIdAndDeletedFalse(batchId).stream()
                .map(ExamDto::from)
                .toList();
    }

    Exam getEntity(UUID id) {
        return examRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exam", id));
    }
}
