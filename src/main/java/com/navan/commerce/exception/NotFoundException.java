package com.navan.commerce.exception;

/** 404 — covers missing transactions, missing invoices, and invoices referencing missing ids. */
public class NotFoundException extends ApiException {

    public NotFoundException(ErrorCode errorCode, String id) {
        super(errorCode, errorCode.getMessage() + ": " + id);
    }
}
