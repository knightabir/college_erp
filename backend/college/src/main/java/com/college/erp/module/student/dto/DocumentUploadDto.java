package com.college.erp.module.student.dto;

import com.college.erp.module.student.domain.DocumentType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Records a document already stored via FileStorageService — fileId comes from that call, not
 * from a multipart body here (the storage module doesn't exist yet; see StudentDocument javadoc).
 */
@Getter
@Setter
public class DocumentUploadDto {

    @NotNull(message = "Document type is required")
    private DocumentType documentType;

    @NotBlank(message = "File id is required")
    private String fileId;
}
