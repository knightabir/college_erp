package com.college.erp.module.transport.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateRouteRequest {

    @NotBlank
    private String name;

    private String description;
}
