package com.navan.commerce.constant;

/**
 * Extension-property keys on RFC 7807 error bodies (e.g. {@code existing_id} on a 409 duplicate).
 * Centralized so the error contract can't drift between exception classes and the handler.
 */
public final class ErrorResponseKeys {

    // ---- standard envelope (every error response has exactly these three first) ----
    public static final String STATUS = "status";
    public static final String STATUS_FAILED = "FAILED";
    public static final String ERROR_CODE = "error_code";
    public static final String ERROR_MESSAGE = "error_message";
    public static final String DEFAULT_ERROR_MESSAGE = "Something went wrong";

    // ---- machine-readable app code + extension fields ----
    public static final String CODE = "code";
    public static final String FIELDS = "fields";
    public static final String EXISTING_ID = "existing_id";
    public static final String TRANSACTION_ID = "transaction_id";
    public static final String CURRENCIES = "currencies";

    private ErrorResponseKeys() {
    }
}
