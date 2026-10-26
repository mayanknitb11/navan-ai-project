package com.navan.commerce.dto;

import java.math.BigDecimal;

/**
 * Contract for ONE money-line element, used on BOTH wires (request and response — the shapes
 * are identical). Each line category has its own concrete DTO ({@link TaxLineEntry},
 * {@link FeeLineEntry}) so category-specific fields (the tax rate) are validated per list —
 * an unknown field on the wrong list is rejected (400) by Jackson fail-on-unknown-properties
 * instead of being normalized away. Never a deserialization target itself: request DTOs
 * declare the concrete per-category lists.
 */
public interface LineEntry {

    String getName();

    BigDecimal getAmount();
}
