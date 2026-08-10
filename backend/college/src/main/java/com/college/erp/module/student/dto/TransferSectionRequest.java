package com.college.erp.module.student.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** 05-api-documentation.md §4 POST /students/{id}/transfer-section. */
@Getter
@Setter
public class TransferSectionRequest {

    @NotNull(message = "New section is required")
    private UUID newSectionId;

    private String remarks;
}
