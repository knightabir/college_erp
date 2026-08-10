package com.college.erp.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Application-level 403. Distinct from Spring Security's own
 * {@code org.springframework.security.access.AccessDeniedException} (which GlobalExceptionHandler
 * also maps to 403) — this one is for business-logic-level denials thrown from services
 * (e.g. "not your own profile") rather than method-security denials.
 */
public class AccessDeniedException extends ErpException {

    public AccessDeniedException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
