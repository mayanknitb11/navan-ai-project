package com.navan.commerce.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.navan.commerce.constant.ValidationConstants;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * PATCH /transactions/{id} body. All fields optional; a present field replaces the stored value.
 * tax_lines / fee_lines use per-category REPLACE-ALL semantics (the DAO bulk-deletes + reinserts
 * in the same transaction — a torn list is impossible).
 *
 * {@code external_id} and {@code type} are deliberately absent: external_id is the identity the
 * duplicate guarantee hangs off, and changing type would orphan the metadata subtype. Unknown
 * fields are rejected (400) by Jackson fail-on-unknown-properties.
 *
 * {@code metadata} is a raw tree here; {@code MetadataValidator} parses it against the entity's
 * actual type and bean-validates the result — a flight cannot be patched into a hotel shape.
 */
@Getter
@Builder
@Jacksonized
public class PatchTransactionRequest {

    @Pattern(regexp = ValidationConstants.CURRENCY_REGEX, message = ValidationConstants.CURRENCY_MESSAGE)
    private final String currency;

    private final Instant occurredAt;

    @DecimalMin(value = ValidationConstants.MIN_AMOUNT, message = ValidationConstants.AMOUNT_RANGE_MESSAGE)
    @DecimalMax(value = ValidationConstants.MAX_AMOUNT, message = ValidationConstants.AMOUNT_RANGE_MESSAGE)
    private final BigDecimal total;

    private final List<@Valid TaxLineEntry> taxLines;

    private final List<@Valid FeeLineEntry> feeLines;

    private final JsonNode metadata;
}
