package com.navan.commerce.domain.metadata.support;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;

/** One flight segment (coupon). A round-trip flight carries several, in order. */
@Getter
@Builder
@Jacksonized
public final class Coupon {

    @NotBlank
    private final String flightNumber;

    private final String carrier;

    @NotBlank
    private final String from;

    @NotBlank
    private final String to;

    @NotNull
    private final Instant departure;

    @NotNull
    private final Instant arrival;
}
