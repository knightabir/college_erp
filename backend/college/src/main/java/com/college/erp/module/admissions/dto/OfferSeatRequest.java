package com.college.erp.module.admissions.dto;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OfferSeatRequest {

    @NotNull
    private UUID batchId;

    private Instant responseDeadline;
}
