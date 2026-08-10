package com.college.erp.module.library.dto;

import java.util.UUID;

import com.college.erp.module.library.domain.BookCopy;
import com.college.erp.module.library.domain.CopyCondition;
import com.college.erp.module.library.domain.CopyStatus;

import lombok.Builder;
import lombok.Getter;

/** 05-api-documentation.md §8 response shape for POST .../copies. */
@Getter
@Builder
public class BookCopyDto {

    private UUID id;
    private UUID titleId;
    private String barcode;
    private String accessionNumber;
    private String shelfLocation;
    private String rackNumber;
    private CopyCondition condition;
    private CopyStatus status;

    public static BookCopyDto from(BookCopy copy) {
        return BookCopyDto.builder()
                .id(copy.getId())
                .titleId(copy.getTitle().getId())
                .barcode(copy.getBarcode())
                .accessionNumber(copy.getAccessionNumber())
                .shelfLocation(copy.getShelfLocation())
                .rackNumber(copy.getRackNumber())
                .condition(copy.getCondition())
                .status(copy.getStatus())
                .build();
    }
}
