package com.navan.commerce.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.navan.commerce.constant.ValidationConstants;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;

/**
 * One {@code tax_lines} element (spec §1): {@code rate} is tax-only and optional.
 * Stored with the rate inside {@code transaction_lines.attributes} (JSONB) — only the
 * shared fields (name, amount) are real columns. Same shape on both wires; a null rate
 * is omitted from responses.
 */
@Getter
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaxLineEntry implements LineEntry {

    @NotBlank
    private final String name;

    private final BigDecimal rate;

    @NotNull
    @DecimalMin(value = ValidationConstants.MIN_AMOUNT, message = ValidationConstants.AMOUNT_RANGE_MESSAGE)
    @DecimalMax(value = ValidationConstants.MAX_AMOUNT, message = ValidationConstants.AMOUNT_RANGE_MESSAGE)
    private final BigDecimal amount;
}
