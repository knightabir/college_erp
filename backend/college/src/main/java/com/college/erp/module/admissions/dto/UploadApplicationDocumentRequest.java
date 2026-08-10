package com.college.erp.module.admissions.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** Records a fileId a prior module.storage upload produced — same pattern as
 *  module.student's StudentDocumentController. */
@Getter
@Setter
public class UploadApplicationDocumentRequest {

    @NotBlank
    private String documentType;

    @NotBlank
    private String fileId;
}
