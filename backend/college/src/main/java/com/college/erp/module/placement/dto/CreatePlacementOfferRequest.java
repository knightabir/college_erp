package com.college.erp.module.placement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePlacementOfferRequest {

    @NotNull
    private BigDecimal ctc;

    private String designation;

    @NotNull
    private LocalDate offerDate;

    private LocalDate joiningDate;
}
