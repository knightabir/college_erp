package com.college.erp.common.dto;

import java.time.Instant;

import lombok.Builder;
import lombok.Getter;

/**
 * Universal success envelope for every REST endpoint. See 05-api-documentation.md §1.2.
 */
@Getter
@Builder
public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;
    @Builder.Default
    private final Instant timestamp = Instant.now();

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder().success(true).data(data).build();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder().success(true).message(message).data(data).build();
    }
}
