package com.navan.commerce.exception;

import com.navan.commerce.constant.ErrorResponseKeys;

import java.util.Map;
import java.util.Set;

/** 409 — an invoice spanning more than one currency. Nothing is persisted. */
public class MixedCurrencyException extends ApiException {

    public MixedCurrencyException(Set<String> currencies) {
        super(ErrorCode.MIXED_CURRENCY,
                ErrorCode.MIXED_CURRENCY.getMessage(),
                Map.of(ErrorResponseKeys.CURRENCIES, currencies));
    }
}
