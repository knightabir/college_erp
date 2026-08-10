package com.college.erp.module.placement.dto;

import com.college.erp.module.placement.domain.DriveApplicationStatus;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateDriveApplicationStatusRequest {

    @NotNull
    private DriveApplicationStatus status;
}
