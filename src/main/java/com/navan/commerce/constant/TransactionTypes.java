package com.navan.commerce.constant;

/**
 * Canonical wire/DB values for transaction types (spec §1). Compile-time constants so they can be
 * used in annotations (@JsonSubTypes) as well as code. The DB CHECK constraint mirrors these.
 */
public final class TransactionTypes {

    public static final String FLIGHT = "flight";
    public static final String HOTEL = "hotel";
    public static final String RAIL = "rail";
    public static final String NAVAN_FEE = "navan_fee";

    private TransactionTypes() {
    }
}
