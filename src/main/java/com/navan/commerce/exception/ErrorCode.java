package com.navan.commerce.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Single source of error truth: every API error is (errorCode, errorMessage, httpStatus).
 * Messages are client-safe by construction — internals never leak into responses.
 */
@Getter
public enum ErrorCode {

    // 400 — client sent something we cannot accept
    VALIDATION_FAILED("VALIDATION_FAILED", "Request validation failed", HttpStatus.BAD_REQUEST),
    UNKNOWN_TYPE("UNKNOWN_TYPE", "Unknown transaction type", HttpStatus.BAD_REQUEST),
    UNKNOWN_FEE_KIND("UNKNOWN_FEE_KIND", "Unknown navan_fee fee_kind", HttpStatus.BAD_REQUEST),

    // 404 — addressed resource does not exist (or is soft-deleted)
    TRANSACTION_NOT_FOUND("TRANSACTION_NOT_FOUND", "Transaction not found", HttpStatus.NOT_FOUND),
    INVOICE_NOT_FOUND("INVOICE_NOT_FOUND", "Invoice not found", HttpStatus.NOT_FOUND),

    // 409 — valid request conflicting with stored state
    DUPLICATE_TRANSACTION("DUPLICATE_TRANSACTION", "A transaction with this external_id already exists", HttpStatus.CONFLICT),
    TRANSACTION_INVOICED("TRANSACTION_INVOICED", "Transaction is part of an invoice and cannot be modified or deleted", HttpStatus.CONFLICT),
    MIXED_CURRENCY("MIXED_CURRENCY", "All transactions on one invoice must share a single currency", HttpStatus.CONFLICT),
    DUPLICATE_TRANSACTION_IDS("DUPLICATE_TRANSACTION_IDS", "The same transaction id is listed more than once", HttpStatus.CONFLICT),
    CONCURRENT_MODIFICATION("CONCURRENT_MODIFICATION", "Resource was modified concurrently; retry the request", HttpStatus.CONFLICT),

    // 500 — never expose internals
    INTERNAL_ERROR("INTERNAL_ERROR", "Unexpected internal error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus status;

    ErrorCode(String code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }
}
