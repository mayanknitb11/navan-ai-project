package com.navan.commerce.dto;

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
 * One {@code fee_lines} element (spec §1): name + amount only, same shape on both wires.
 * Fees carry no rate — a {@code rate} key sent here is rejected (400) by
 * fail-on-unknown-properties.
 */
@Getter
@Builder
@Jacksonized
public class FeeLineEntry implements LineEntry {

    @NotBlank
    private final String name;

    @NotNull
    @DecimalMin(value = ValidationConstants.MIN_AMOUNT, message = ValidationConstants.AMOUNT_RANGE_MESSAGE)
    @DecimalMax(value = ValidationConstants.MAX_AMOUNT, message = ValidationConstants.AMOUNT_RANGE_MESSAGE)
    private final BigDecimal amount;
}
