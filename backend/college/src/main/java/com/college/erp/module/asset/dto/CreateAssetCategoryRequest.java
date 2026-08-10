package com.college.erp.module.asset.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateAssetCategoryRequest {

    @NotBlank
    private String name;

    @Min(0)
    @Max(100)
    private Integer depreciationRatePct;
}
