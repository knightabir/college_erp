package com.college.erp.common.exception;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public class ValidationException extends ErpException {

    private final Map<String, List<String>> fieldErrors;

    public ValidationException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
        this.fieldErrors = Map.of();
    }

    public ValidationException(String message, Map<String, List<String>> fieldErrors) {
        super(message, HttpStatus.BAD_REQUEST);
        this.fieldErrors = fieldErrors;
    }
}
