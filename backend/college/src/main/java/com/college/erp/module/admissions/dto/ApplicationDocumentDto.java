package com.college.erp.module.admissions.dto;

import java.time.Instant;
import java.util.UUID;

import com.college.erp.module.admissions.domain.ApplicationDocument;
import com.college.erp.module.admissions.domain.DocumentVerificationStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApplicationDocumentDto {

    private UUID id;
    private UUID applicationId;
    private String documentType;
    private String fileId;
    private DocumentVerificationStatus verificationStatus;
    private UUID verifiedBy;
    private Instant verifiedAt;

    public static ApplicationDocumentDto from(ApplicationDocument document) {
        return ApplicationDocumentDto.builder()
                .id(document.getId())
                .applicationId(document.getApplication().getId())
                .documentType(document.getDocumentType())
                .fileId(document.getFileId())
                .verificationStatus(document.getVerificationStatus())
                .verifiedBy(document.getVerifiedBy())
                .verifiedAt(document.getVerifiedAt())
                .build();
    }
}
