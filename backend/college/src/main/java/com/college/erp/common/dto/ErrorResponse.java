package com.college.erp.common.dto;

import java.time.Instant;
import java.util.Map;

import lombok.Builder;
import lombok.Getter;

/**
 * RFC 7807 Problem Details error shape. See 05-api-documentation.md §1.4.
 */
@Getter
@Builder
public class ErrorResponse {

    private final String type;
    private final String title;
    private final int status;
    private final String detail;
    private final String instance;
    private final String requestId;
    @Builder.Default
    private final Instant timestamp = Instant.now();
    private final Map<String, java.util.List<String>> fieldErrors;
}
