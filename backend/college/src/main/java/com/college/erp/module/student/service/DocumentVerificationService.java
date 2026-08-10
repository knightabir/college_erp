package com.college.erp.module.student.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.module.student.domain.DocumentVerificationStatus;
import com.college.erp.module.student.domain.Student;
import com.college.erp.module.student.domain.StudentDocument;
import com.college.erp.module.student.domain.StudentDocumentRepository;
import com.college.erp.module.student.dto.DocumentUploadDto;
import com.college.erp.module.student.dto.StudentDocumentDto;
import com.college.erp.tenant.TenantContext;

import lombok.RequiredArgsConstructor;

/**
 * Records and verifies documents whose bytes are already stored via FileStorageService (not
 * built yet — see StudentDocument javadoc). "Upload" here means "record the fileId a prior
 * storage call returned," not a multipart handler.
 */
@Service
@RequiredArgsConstructor
public class DocumentVerificationService {

    private static final String MODULE = "STUDENT";

    private final StudentDocumentRepository studentDocumentRepository;
    private final StudentService studentService;
    private final AuditLogWriter auditLogWriter;

    @Transactional
    public StudentDocumentDto add(UUID studentId, DocumentUploadDto request) {
        Student student = studentService.getStudentEntity(studentId);

        StudentDocument document = StudentDocument.builder()
                .student(student)
                .documentType(request.getDocumentType())
                .fileId(request.getFileId())
                .build();
        document.setTenantId(TenantContext.getCurrentTenantId());

        StudentDocument saved = studentDocumentRepository.save(document);
        auditLogWriter.write(AuditAction.CREATE, MODULE, "StudentDocument", saved.getId().toString(),
                "Recorded " + saved.getDocumentType() + " for student " + student.getEnrollmentNumber());

        return StudentDocumentDto.from(saved);
    }

    @Transactional
    public StudentDocumentDto verify(UUID documentId, UUID verifierUserId) {
        StudentDocument document = getEntity(documentId);
        document.setVerificationStatus(DocumentVerificationStatus.VERIFIED);
        document.setVerifiedBy(verifierUserId);
        document.setVerifiedAt(Instant.now());
        document.setRemarks(null);

        StudentDocument saved = studentDocumentRepository.save(document);
        auditLogWriter.write(AuditAction.UPDATE, MODULE, "StudentDocument", saved.getId().toString(),
                "Verified document " + saved.getDocumentType());

        return StudentDocumentDto.from(saved);
    }

    @Transactional
    public StudentDocumentDto reject(UUID documentId, UUID verifierUserId, String remarks) {
        StudentDocument document = getEntity(documentId);
        document.setVerificationStatus(DocumentVerificationStatus.REJECTED);
        document.setVerifiedBy(verifierUserId);
        document.setVerifiedAt(Instant.now());
        document.setRemarks(remarks);

        StudentDocument saved = studentDocumentRepository.save(document);
        auditLogWriter.write(AuditAction.UPDATE, MODULE, "StudentDocument", saved.getId().toString(),
                "Rejected document " + saved.getDocumentType() + ": " + remarks);

        return StudentDocumentDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<StudentDocumentDto> listByStudent(UUID studentId) {
        return studentDocumentRepository.findByStudentIdAndDeletedFalse(studentId).stream()
                .map(StudentDocumentDto::from)
                .toList();
    }

    private StudentDocument getEntity(UUID id) {
        return studentDocumentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("StudentDocument", id));
    }
}
