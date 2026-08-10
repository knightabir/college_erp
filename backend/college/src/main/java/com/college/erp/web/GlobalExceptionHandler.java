package com.college.erp.web;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import com.college.erp.common.dto.ErrorResponse;
import com.college.erp.common.exception.ErpException;

import lombok.extern.slf4j.Slf4j;

/**
 * Single place that maps every typed exception to an RFC 7807 response (05-api-documentation.md
 * §1.4). Controllers and services never try/catch for this purpose — they throw typed exceptions
 * and let this handle it once (07-coding-standards.md §11.1).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String ERROR_BASE_URI = "https://erp.college.edu/errors/";

    @ExceptionHandler(ErpException.class)
    public ResponseEntity<ErrorResponse> handleErpException(ErpException ex, WebRequest request) {
        HttpStatus status = ex.getStatus();
        if (status.is5xxServerError()) {
            log.error("Unhandled business exception: {}", ex.getMessage(), ex);
        } else {
            log.warn("{}: {}", ex.getClass().getSimpleName(), ex.getMessage());
        }

        Map<String, List<String>> fieldErrors = null;
        if (ex instanceof com.college.erp.common.exception.ValidationException ve) {
            fieldErrors = ve.getFieldErrors().isEmpty() ? null : ve.getFieldErrors();
        }

        return build(status, errorType(ex), ex.getMessage(), path(request), fieldErrors);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, List<String>> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.groupingBy(
                        FieldError::getField,
                        Collectors.mapping(fe -> fe.getDefaultMessage() == null ? "invalid" : fe.getDefaultMessage(),
                                Collectors.toList())));

        return build(HttpStatus.BAD_REQUEST, "validation", "One or more fields failed validation",
                path(request), fieldErrors);
    }

    /**
     * Covers malformed JSON bodies (bad character escapes, unexpected tokens, missing
     * Content-Type header so the body is never parsed, etc.).
     * These are client mistakes — a 400 with a plain-English detail is enough; no stack
     * trace needed in the logs.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex, WebRequest request) {
        String detail = buildReadableDetail(ex);
        log.warn("Unreadable HTTP message: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "malformed-request", detail, path(request), null);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            org.springframework.security.access.AccessDeniedException ex, WebRequest request) {
        return build(HttpStatus.FORBIDDEN, "forbidden", "You do not have permission to perform this action",
                path(request), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, WebRequest request) {
        log.error("Unexpected error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "internal", "An unexpected error occurred",
                path(request), null);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String type, String detail, String instance,
            Map<String, List<String>> fieldErrors) {
        ErrorResponse body = ErrorResponse.builder()
                .type(ERROR_BASE_URI + type)
                .title(status.getReasonPhrase())
                .status(status.value())
                .detail(detail)
                .instance(instance)
                .requestId(MDC.get(RequestIdFilter.MDC_REQUEST_ID))
                .fieldErrors(fieldErrors)
                .build();
        return ResponseEntity.status(status).body(body);
    }

    /**
     * Extracts a short, client-safe description from a Jackson parse failure without
     * leaking internal class names or stack frames.
     */
    private String buildReadableDetail(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof com.fasterxml.jackson.core.JsonProcessingException jpe) {
            // e.g. "Unrecognized character escape '''' (code 39) at [Source: (org.springframework…"
            // Strip everything from " at [Source" onward — clients don't need the source path.
            String msg = jpe.getOriginalMessage();
            if (msg != null) {
                int atIdx = msg.indexOf(" at [Source");
                return "Malformed JSON: " + (atIdx > 0 ? msg.substring(0, atIdx) : msg);
            }
        }
        if (ex.getMessage() != null && ex.getMessage().contains("Required request body is missing")) {
            return "Request body is missing. Make sure you are sending a JSON body with Content-Type: application/json";
        }
        return "The request body could not be parsed. Ensure it is valid JSON and the Content-Type header is set to application/json";
    }

    private String errorType(ErpException ex) {
        return ex.getClass().getSimpleName()
                .replace("Exception", "")
                .replaceAll("([a-z])([A-Z])", "$1-$2")
                .toLowerCase();
    }

    private String path(WebRequest request) {
        if (request instanceof org.springframework.web.context.request.ServletWebRequest servletRequest) {
            return servletRequest.getRequest().getRequestURI();
        }
        return request.getDescription(false).replace("uri=", "");
    }
}
