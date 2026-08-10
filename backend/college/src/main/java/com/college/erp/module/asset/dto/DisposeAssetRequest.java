package com.college.erp.module.asset.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** 05-api-documentation.md §9: PATCH /assets/{id}/dispose. */
@Getter
@Setter
public class DisposeAssetRequest {

    @NotBlank
    private String reason;
}
