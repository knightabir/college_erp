package com.college.erp.module.library.dto;

import java.util.UUID;

import com.college.erp.module.library.domain.BorrowerType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** 05-api-documentation.md §8 POST /library/circulation/issue. */
@Getter
@Setter
public class IssueBookDto {

    @NotBlank(message = "Barcode is required")
    private String barcode;

    @NotNull(message = "Borrower is required")
    private UUID borrowerUserId;

    @NotBlank(message = "Borrower name is required")
    private String borrowerName;

    @NotNull(message = "Borrower type is required")
    private BorrowerType borrowerType;
}
