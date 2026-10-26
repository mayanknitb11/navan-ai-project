package com.navan.commerce.constant;

/** Shared validation patterns/messages used across request DTOs. */
public final class ValidationConstants {

    /** ISO-4217 style 3-letter uppercase currency code. Message omits the field name — the
     *  error envelope prepends it ("currency must be …"), so repeating it would double-print. */
    public static final String CURRENCY_REGEX = "^[A-Z]{3}$";
    public static final String CURRENCY_MESSAGE = "must be a 3-letter ISO code (uppercase)";

    /** Bounds matching the NUMERIC(19,2) money columns — larger magnitudes overflow at flush
     *  time; validating here turns that into a 400 instead of a misleading 409. */
    public static final String MAX_AMOUNT = "99999999999999999.99";
    public static final String MIN_AMOUNT = "-99999999999999999.99";
    public static final String AMOUNT_RANGE_MESSAGE = "is outside the storable amount range (17 integer digits, scale 2)";

    private ValidationConstants() {
    }
}
