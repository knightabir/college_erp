package com.college.erp.module.attendance.dto;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** 05-api-documentation.md §5 POST /attendance/sessions. */
@Getter
@Setter
public class CreateSessionRequest {

    @NotNull(message = "Subject offering is required")
    private UUID offeringId;

    @NotNull(message = "Section is required")
    private UUID sectionId;

    @NotNull(message = "Session date is required")
    private LocalDate sessionDate;
}
