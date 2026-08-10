package com.college.erp.module.hr.dto;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** 05-api-documentation.md §10: POST /hr/leave/apply. */
@Getter
@Setter
public class ApplyLeaveRequest {

    @NotNull
    private UUID leaveTypeId;

    @NotNull
    private LocalDate fromDate;

    @NotNull
    private LocalDate toDate;

    private String reason;
}
