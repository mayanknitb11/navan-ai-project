package com.navan.commerce.constant;

/**
 * Canonical DB values for line categories (the discriminator on the merged lines tables).
 * A new category (e.g. "discount") = one constant + one enum value + one mapper branch.
 */
public final class LineCategories {

    public static final String TAX = "tax";
    public static final String FEE = "fee";

    private LineCategories() {
    }
}
