package com.college.erp.common.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

/**
 * Base type for every typed application exception. GlobalExceptionHandler maps these to HTTP
 * responses by status — never catch-and-rethrow as a raw RuntimeException (07-coding-standards.md §11.1).
 */
@Getter
public abstract class ErpException extends RuntimeException {

    private final HttpStatus status;

    protected ErpException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}
