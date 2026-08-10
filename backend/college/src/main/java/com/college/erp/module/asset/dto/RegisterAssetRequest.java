package com.college.erp.module.asset.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** 05-api-documentation.md §9: POST /assets. */
@Getter
@Setter
public class RegisterAssetRequest {

    @NotBlank
    private String name;

    private String description;

    private UUID categoryId;

    @NotNull
    private UUID departmentId;

    private String roomLocation;

    private LocalDate purchaseDate;

    private BigDecimal purchaseCost;

    private UUID vendorId;

    private LocalDate warrantyExpiry;

    private UUID assignedToUserId;
}
