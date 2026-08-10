package com.college.erp.module.student.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RejectDocumentRequest {

    @NotBlank(message = "Remarks are required when rejecting a document")
    private String remarks;
}
