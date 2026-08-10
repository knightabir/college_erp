package com.college.erp.module.communication.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignTicketRequest {

    @NotNull
    private UUID assigneeUserId;
}
