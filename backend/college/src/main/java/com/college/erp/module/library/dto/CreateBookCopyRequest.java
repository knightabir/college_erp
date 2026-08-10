package com.college.erp.module.library.dto;

import lombok.Getter;
import lombok.Setter;

/** 05-api-documentation.md §8 POST /library/catalog/titles/{titleId}/copies. */
@Getter
@Setter
public class CreateBookCopyRequest {

    private String shelfLocation;
    private String rackNumber;
}
