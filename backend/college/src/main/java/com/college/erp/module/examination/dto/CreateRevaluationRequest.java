package com.college.erp.module.examination.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** 05-api-documentation.md §6 POST /exams/revaluation. */
@Getter
@Setter
public class CreateRevaluationRequest {

    @NotNull(message = "Marks entry is required")
    private UUID marksEntryId;

    @NotBlank(message = "Reason is required")
    private String reason;
}
