package com.navan.commerce.exception;

import com.navan.commerce.constant.ErrorResponseKeys;

import java.util.Map;

/**
 * 409 on duplicate {@code external_id} (D3). The body carries the EXISTING resource id,
 * as the spec requires for duplicate creates.
 */
public class DuplicateTransactionException extends ApiException {

    public DuplicateTransactionException(String existingId) {
        super(ErrorCode.DUPLICATE_TRANSACTION,
                ErrorCode.DUPLICATE_TRANSACTION.getMessage(),
                Map.of(ErrorResponseKeys.EXISTING_ID, existingId));
    }
}
