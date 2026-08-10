package com.college.erp.module.academic.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateSubjectOfferingRequest {

    @NotNull(message = "Batch is required")
    private UUID batchId;

    @NotNull(message = "Subject is required")
    private UUID subjectId;

    @NotNull(message = "Term is required")
    private UUID termId;

    private Integer maxInternalMarks;
    private Integer maxExternalMarks;
    private Integer passMarksInternal;
    private Integer passMarksExternal;
    private boolean elective;
}
