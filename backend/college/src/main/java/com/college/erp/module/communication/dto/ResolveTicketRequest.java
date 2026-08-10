package com.college.erp.module.communication.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResolveTicketRequest {

    @NotBlank
    private String resolution;
}
