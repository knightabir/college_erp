package com.college.erp.module.communication.dto;

import com.college.erp.module.communication.domain.TicketCategory;
import com.college.erp.module.communication.domain.TicketPriority;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** 05-api-documentation.md §11: POST /helpdesk. */
@Getter
@Setter
public class RaiseTicketRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String description;

    @NotNull
    private TicketCategory category;

    private TicketPriority priority;
}
