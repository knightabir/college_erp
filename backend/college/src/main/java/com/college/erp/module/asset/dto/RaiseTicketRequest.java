package com.college.erp.module.asset.dto;

import com.college.erp.module.asset.domain.TicketPriority;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** 05-api-documentation.md §9: POST /assets/{id}/maintenance-tickets — any authenticated user. */
@Getter
@Setter
public class RaiseTicketRequest {

    @NotBlank
    private String title;

    private String description;

    private TicketPriority priority;
}
