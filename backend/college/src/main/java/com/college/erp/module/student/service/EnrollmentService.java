package com.college.erp.module.student.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.BusinessRuleViolationException;
import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.module.academic.domain.Batch;
import com.college.erp.module.academic.domain.Section;
import com.college.erp.module.academic.service.BatchService;
import com.college.erp.module.academic.service.SectionService;
import com.college.erp.module.academic.service.TermService;
import com.college.erp.module.student.domain.Enrollment;
import com.college.erp.module.student.domain.EnrollmentRepository;
import com.college.erp.module.student.domain.EnrollmentStatus;
import com.college.erp.module.student.domain.Student;
import com.college.erp.module.student.dto.EnrollmentDto;
import com.college.erp.module.student.dto.TransferSectionRequest;
import com.college.erp.tenant.TenantContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 04-database-schema.md §7.2 "key rule" — before inserting a new enrollment, the previous current
 * row for the student is flipped to current = false, in the same transaction as the insert.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private static final String MODULE = "STUDENT";

    private final EnrollmentRepository enrollmentRepository;
    private final BatchService batchService;
    private final SectionService sectionService;
    private final TermService termService;
    private final AuditLogWriter auditLogWriter;

    /** Called once, from StudentService.create(), inside the same transaction as the Student
     *  insert — student enrollment always starts in the batch's current term. */
    @Transactional
    public Enrollment createInitialEnrollment(Student student, UUID batchId, UUID sectionId) {
        Batch batch = batchService.getBatchEntity(batchId);
        Section section = sectionService.getSectionEntity(sectionId);

        if (!section.getBatch().getId().equals(batch.getId())) {
            throw new BusinessRuleViolationException("Section does not belong to the given batch");
        }

        Enrollment enrollment = Enrollment.builder()
                .student(student)
                .batch(batch)
                .section(section)
                .term(termService.getCurrentTermEntityForBatch(batch))
                .enrollmentDate(LocalDate.now())
                .status(EnrollmentStatus.ACTIVE)
                .current(true)
                .build();
        enrollment.setTenantId(TenantContext.getCurrentTenantId());

        return enrollmentRepository.save(enrollment);
    }

    @Transactional
    public EnrollmentDto transferSection(UUID studentId, TransferSectionRequest request) {
        Enrollment current = getCurrentEntity(studentId);
        Section newSection = sectionService.getSectionEntity(request.getNewSectionId());

        if (!newSection.getBatch().getId().equals(current.getBatch().getId())) {
            throw new BusinessRuleViolationException(
                    "Cannot transfer to a section in a different batch — use promotion instead");
        }

        current.setCurrent(false);
        current.setStatus(EnrollmentStatus.TRANSFERRED);
        enrollmentRepository.save(current);

        Enrollment transferred = Enrollment.builder()
                .student(current.getStudent())
                .batch(current.getBatch())
                .section(newSection)
                .term(current.getTerm())
                .enrollmentDate(LocalDate.now())
                .status(EnrollmentStatus.ACTIVE)
                .current(true)
                .remarks(request.getRemarks())
                .build();
        transferred.setTenantId(TenantContext.getCurrentTenantId());
        Enrollment saved = enrollmentRepository.save(transferred);

        auditLogWriter.write(AuditAction.UPDATE, MODULE, "Enrollment", saved.getId().toString(),
                "Transferred student " + current.getStudent().getEnrollmentNumber()
                        + " to section " + newSection.getName());
        log.info("Student [{}] transferred to section [{}]",
                current.getStudent().getEnrollmentNumber(), newSection.getName());

        return EnrollmentDto.from(saved);
    }

    @Transactional(readOnly = true)
    public EnrollmentDto getCurrent(UUID studentId) {
        return EnrollmentDto.from(getCurrentEntity(studentId));
    }

    @Transactional(readOnly = true)
    public List<EnrollmentDto> getHistory(UUID studentId) {
        return enrollmentRepository.findHistoryByStudentId(studentId).stream()
                .map(EnrollmentDto::from)
                .toList();
    }

    private Enrollment getCurrentEntity(UUID studentId) {
        return enrollmentRepository.findByStudentIdAndCurrentTrueAndDeletedFalse(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Current enrollment for student", studentId));
    }
}
