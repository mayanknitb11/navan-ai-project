package com.navan.commerce.domain.metadata.support;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;

/**
 * One rail leg. {@code travelClass} maps the JSON field literally named {@code "class"}
 * (a Java keyword) — without {@code @JsonProperty} the field would silently drop on round-trip.
 */
@Getter
@Builder
@Jacksonized
public final class Leg {

    @NotBlank
    private final String origin;

    @NotBlank
    private final String destination;

    @NotNull
    private final Instant departure;

    @NotNull
    private final Instant arrival;

    private final String trainNumber;

    @JsonProperty("class")
    private final String travelClass;
}
