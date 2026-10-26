package com.navan.commerce.domain.metadata;

import com.navan.commerce.domain.metadata.support.Coupon;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.util.List;

/**
 * Flight: origin/destination, fare, and coupons (segments). Round-trips carry more than one
 * coupon. The list itself may be empty — the duplicate-seed fixture legitimately sends
 * {@code "coupons": []}, and validation must not turn that retry into a 400 before the
 * duplicate check can answer 409.
 */
@Getter
@Builder
@Jacksonized
public final class FlightMetadata implements TransactionMetadata {

    @NotBlank
    private final String origin;

    @NotBlank
    private final String destination;

    @NotNull
    private final BigDecimal fare;

    @NotNull
    private final List<@Valid Coupon> coupons;
}
