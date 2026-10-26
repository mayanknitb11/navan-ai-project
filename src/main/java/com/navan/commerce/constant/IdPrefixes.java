package com.navan.commerce.constant;

/** Server id prefixes (D7): ids are self-describing in logs. Column width VARCHAR(40) fits prefix + UUID. */
public final class IdPrefixes {

    public static final String TRANSACTION = "txn_";
    public static final String INVOICE = "inv_";

    private IdPrefixes() {
    }
}
