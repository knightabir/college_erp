package com.college.erp.common.exception;

import org.springframework.http.HttpStatus;

public class BusinessRuleViolationException extends ErpException {

    public BusinessRuleViolationException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
