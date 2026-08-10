package com.college.erp.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ErpException {

    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }

    public ResourceNotFoundException(String entityName, Object id) {
        super(entityName + " not found: " + id, HttpStatus.NOT_FOUND);
    }
}
