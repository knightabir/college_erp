package com.college.erp.module.examination.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** Not documented explicitly (API doc only shows apply, not review) — follows the same
 *  request-shape conventions as attendance condonation review. */
@Getter
@Setter
public class CompleteRevaluationRequest {

    @NotNull(message = "Decision is required")
    private Boolean approved;

    /** Corrected external marks — only meaningful when approved is true and the mark changed. */
    private BigDecimal reviewedMarks;
}
