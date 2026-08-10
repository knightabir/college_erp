package com.college.erp.module.library.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WaiveFineRequest {

    @NotBlank(message = "Waiver reason is required")
    private String reason;
}
