package com.navan.commerce.constant;

/**
 * Database-facing literals shared by entity annotations and DAO query composition.
 * Compile-time constants, safe to use inside annotations (e.g. @Table).
 */
public final class DatabaseConstants {

    // ---- table names ----
    public static final String TABLE_TRANSACTIONS = "transactions";
    public static final String TABLE_TRANSACTION_LINES = "transaction_lines";
    public static final String TABLE_INVOICES = "invoices";
    public static final String TABLE_INVOICE_LINES = "invoice_lines";

    // ---- entity attribute names for stable page ordering (backed by idx_*_created_id) ----
    public static final String SORT_CREATED_AT = "createdAt";
    public static final String SORT_ID = "id";

    private DatabaseConstants() {
    }
}
