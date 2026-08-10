package com.college.erp.common.exception;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends ErpException {

    public DuplicateResourceException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
