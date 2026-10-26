package com.navan.commerce.exception;

import com.navan.commerce.constant.ErrorResponseKeys;

import java.util.Map;

/** 409 — PATCH/DELETE attempted on a transaction that is already on a non-deleted invoice. */
public class TransactionInvoicedException extends ApiException {

    public TransactionInvoicedException(String transactionId) {
        super(ErrorCode.TRANSACTION_INVOICED,
                ErrorCode.TRANSACTION_INVOICED.getMessage(),
                Map.of(ErrorResponseKeys.TRANSACTION_ID, transactionId));
    }
}
