package com.college.erp.module.attendance.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** 05-api-documentation.md §5 POST /attendance/condonation. */
@Getter
@Setter
public class CreateCondonationRequest {

    @NotNull(message = "Subject offering is required")
    private UUID offeringId;

    @NotNull(message = "Term is required")
    private UUID termId;

    @NotBlank(message = "Reason is required")
    private String reason;
}
