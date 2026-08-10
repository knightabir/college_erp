package com.college.erp.module.student.dto;

import java.time.Instant;
import java.util.UUID;

import com.college.erp.module.student.domain.DocumentType;
import com.college.erp.module.student.domain.DocumentVerificationStatus;
import com.college.erp.module.student.domain.StudentDocument;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StudentDocumentDto {

    private UUID id;
    private UUID studentId;
    private DocumentType documentType;
    private String fileId;
    private DocumentVerificationStatus verificationStatus;
    private UUID verifiedBy;
    private Instant verifiedAt;
    private String remarks;

    public static StudentDocumentDto from(StudentDocument document) {
        return StudentDocumentDto.builder()
                .id(document.getId())
                .studentId(document.getStudent().getId())
                .documentType(document.getDocumentType())
                .fileId(document.getFileId())
                .verificationStatus(document.getVerificationStatus())
                .verifiedBy(document.getVerifiedBy())
                .verifiedAt(document.getVerifiedAt())
                .remarks(document.getRemarks())
                .build();
    }
}
