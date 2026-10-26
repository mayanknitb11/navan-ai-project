package com.navan.commerce.dto;

import com.navan.commerce.domain.LineCategory;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;

/**
 * The canonical money-line shape, shared by the transaction flow and the invoice snapshot
 * ({@code invoice_lines.breakdown} stores a list of these via {@code LineBreakdownConverter}).
 *
 * ONE shape for every line category: adding a category (or an attribute) on the transaction
 * side flows to invoices automatically — no invoice-side code changes. The API contract still
 * exposes separate tax_lines / fee_lines lists (spec L31); mappers split by {@code category}.
 */
@Getter
@Builder
@Jacksonized
public class LineItem {

    private final LineCategory category;

    private final String name;

    /** Tax-only (spec's optional rate); null for fees. */
    private final BigDecimal rate;

    private final BigDecimal amount;
}
