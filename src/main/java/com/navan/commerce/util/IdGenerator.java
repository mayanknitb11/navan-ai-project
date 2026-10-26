package com.navan.commerce.util;

import com.navan.commerce.constant.IdPrefixes;

import java.util.UUID;

/**
 * Prefixed UUIDs (D7): self-describing in logs, no sequence contention under concurrent creates.
 * Column width VARCHAR(40) fits prefix + 36-char UUID.
 */
public final class IdGenerator {

    private IdGenerator() {
    }

    public static String newTransactionId() {
        return IdPrefixes.TRANSACTION + UUID.randomUUID();
    }

    public static String newInvoiceId() {
        return IdPrefixes.INVOICE + UUID.randomUUID();
    }

    /** Plain UUID for child line rows — never exposed via the API, only primary keys. */
    public static String newId() {
        return UUID.randomUUID().toString();
    }
}
