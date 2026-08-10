package com.college.erp.module.hr.dto;

import lombok.Getter;
import lombok.Setter;

/** 05-api-documentation.md §10: PATCH /hr/leave/{requestId}/approve|reject. */
@Getter
@Setter
public class ReviewLeaveRequest {

    private String remarks;
}
