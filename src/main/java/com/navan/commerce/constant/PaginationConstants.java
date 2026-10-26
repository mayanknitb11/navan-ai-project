package com.navan.commerce.constant;

/**
 * Pagination bounds (spec §5, decision D5): limit defaults to 50, valid range 1..100
 * (limit=1000 → 400), offset ≥ 0. Strings exist because @RequestParam defaultValue takes one.
 */
public final class PaginationConstants {

    public static final long MIN_LIMIT = 1;
    public static final long MAX_LIMIT = 100;
    public static final long MIN_OFFSET = 0;

    public static final String DEFAULT_LIMIT = "50";
    public static final String DEFAULT_OFFSET = "0";

    private PaginationConstants() {
    }
}
