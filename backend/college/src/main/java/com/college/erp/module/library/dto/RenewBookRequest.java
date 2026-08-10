package com.college.erp.module.library.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** 05-api-documentation.md §8 POST /library/circulation/renew. */
@Getter
@Setter
public class RenewBookRequest {

    @NotBlank(message = "Barcode is required")
    private String barcode;
}
