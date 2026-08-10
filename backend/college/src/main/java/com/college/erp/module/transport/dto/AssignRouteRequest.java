package com.college.erp.module.transport.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignRouteRequest {

    @NotNull
    private UUID studentId;

    @NotNull
    private UUID routeId;

    private UUID vehicleId;
    private String stopName;
}
