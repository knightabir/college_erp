package com.college.erp.module.attendance.dto;

import com.college.erp.module.attendance.domain.CondonationStatus;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** 05-api-documentation.md §5 PATCH /attendance/condonation/{requestId}. */
@Getter
@Setter
public class ReviewCondonationRequest {

    @NotNull(message = "Decision is required")
    private CondonationStatus decision;

    private String remarks;
}
