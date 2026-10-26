package com.navan.commerce.domain.metadata.support;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

/** Hotel property address — street/city/country are the spec minimum. */
@Getter
@Builder
@Jacksonized
public final class Address {

    @NotBlank
    private final String street;

    @NotBlank
    private final String city;

    @NotBlank
    private final String country;
}
