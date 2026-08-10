package com.college.erp.module.admissions.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateLateralEntryDetailsRequest {

    private String diplomaInstitution;
    private BigDecimal diplomaScore;
    private Integer entryYearNumber;
}
