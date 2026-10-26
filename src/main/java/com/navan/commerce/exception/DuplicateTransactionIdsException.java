package com.navan.commerce.exception;

/** 409 — the same transaction id listed twice on one invoice (D4; never silently double-counted). */
public class DuplicateTransactionIdsException extends ApiException {

    public DuplicateTransactionIdsException() {
        super(ErrorCode.DUPLICATE_TRANSACTION_IDS);
    }
}
