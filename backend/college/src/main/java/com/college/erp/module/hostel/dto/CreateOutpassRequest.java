package com.college.erp.module.hostel.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateOutpassRequest {

    @NotNull
    private Instant fromDatetime;

    @NotNull
    private Instant toDatetime;

    @NotBlank
    private String reason;

    private String destination;
}
