package com.college.erp.module.library.dto;

import com.college.erp.module.library.domain.CopyCondition;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** 05-api-documentation.md §8 POST /library/circulation/return. */
@Getter
@Setter
public class ReturnBookDto {

    @NotBlank(message = "Barcode is required")
    private String barcode;

    @NotNull(message = "Condition on return is required")
    private CopyCondition conditionOnReturn;
}
