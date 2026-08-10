package com.college.erp.module.student.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogWriter;
import com.college.erp.module.student.domain.Guardian;
import com.college.erp.module.student.domain.GuardianRepository;
import com.college.erp.module.student.domain.Student;
import com.college.erp.module.student.dto.CreateGuardianRequest;
import com.college.erp.module.student.dto.GuardianDto;
import com.college.erp.tenant.TenantContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GuardianService {

    private static final String MODULE = "STUDENT";

    private final GuardianRepository guardianRepository;
    private final StudentService studentService;
    private final AuditLogWriter auditLogWriter;

    @Transactional
    public GuardianDto add(UUID studentId, CreateGuardianRequest request) {
        Student student = studentService.getStudentEntity(studentId);

        Guardian guardian = Guardian.builder()
                .student(student)
                .name(request.getName())
                .relationship(request.getRelationship())
                .phone(request.getPhone())
                .email(request.getEmail())
                .occupation(request.getOccupation())
                .annualIncome(request.getAnnualIncome())
                .primary(request.isPrimary())
                .build();
        guardian.setTenantId(TenantContext.getCurrentTenantId());

        Guardian saved = guardianRepository.save(guardian);
        auditLogWriter.write(AuditAction.CREATE, MODULE, "Guardian", saved.getId().toString(),
                "Added guardian " + saved.getName() + " for student " + student.getEnrollmentNumber());

        return GuardianDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<GuardianDto> listByStudent(UUID studentId) {
        return guardianRepository.findByStudentIdAndDeletedFalse(studentId).stream()
                .map(GuardianDto::from)
                .toList();
    }
}
