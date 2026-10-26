package com.navan.commerce.exception;

import lombok.Getter;

import java.util.Map;

/**
 * Base for all deliberate API errors. Carries an {@link ErrorCode} (which owns the HTTP status)
 * plus optional client-safe detail properties (e.g. {@code existing_id} for duplicates).
 */
@Getter
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Map<String, Object> details;

    public ApiException(ErrorCode errorCode) {
        this(errorCode, errorCode.getMessage(), Map.of());
    }

    public ApiException(ErrorCode errorCode, String message) {
        this(errorCode, message, Map.of());
    }

    public ApiException(ErrorCode errorCode, String message, Map<String, Object> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }
}
