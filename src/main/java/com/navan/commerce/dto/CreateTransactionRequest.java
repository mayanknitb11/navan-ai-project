package com.navan.commerce.dto;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.navan.commerce.constant.ValidationConstants;
import com.navan.commerce.domain.TransactionType;
import com.navan.commerce.domain.metadata.TransactionMetadata;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * POST /transactions body (spec §1). The polymorphic {@code metadata} subtype is selected by the
 * sibling {@code type} property via Jackson EXTERNAL_PROPERTY — an unknown type, or metadata that
 * doesn't match the declared type, fails deserialization and becomes a 400, so a flight can never
 * be stored as a hotel blob. tax_lines / fee_lines are required keys but may be empty lists.
 *
 * Immutable: built by Jackson via the builder (@Jacksonized) or by tests via the same builder.
 */
@Getter
@Builder
@Jacksonized
public class CreateTransactionRequest {

    @NotNull
    private final TransactionType type;

    @NotBlank
    @Size(max = 255)
    private final String externalId;

    @NotBlank
    @Pattern(regexp = ValidationConstants.CURRENCY_REGEX, message = ValidationConstants.CURRENCY_MESSAGE)
    private final String currency;

    @NotNull
    private final Instant occurredAt;

    @NotNull
    @DecimalMin(value = ValidationConstants.MIN_AMOUNT, message = ValidationConstants.AMOUNT_RANGE_MESSAGE)
    @DecimalMax(value = ValidationConstants.MAX_AMOUNT, message = ValidationConstants.AMOUNT_RANGE_MESSAGE)
    private final BigDecimal total;

    @NotNull
    private final List<@Valid TaxLineEntry> taxLines;

    @NotNull
    private final List<@Valid FeeLineEntry> feeLines;

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type")
    @NotNull
    @Valid
    private final TransactionMetadata metadata;
}
