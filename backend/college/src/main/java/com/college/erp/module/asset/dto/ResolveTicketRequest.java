package com.college.erp.module.asset.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** 05-api-documentation.md §9: PATCH /assets/tickets/{ticketId}/resolve. */
@Getter
@Setter
public class ResolveTicketRequest {

    @NotBlank
    private String resolution;
}
