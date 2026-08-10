package com.college.erp.module.admissions.dto;

import com.college.erp.module.admissions.domain.ApplicationStatus;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** INVENTED — see V015's header comment. */
@Getter
@Setter
public class ReviewApplicationRequest {

    @NotNull
    private ApplicationStatus status;

    private String remarks;
}
