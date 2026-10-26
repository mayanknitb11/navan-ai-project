package com.navan.commerce.domain.metadata.support;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;

/** Optional hotel extra (e.g. breakfast). */
@Getter
@Builder
@Jacksonized
public final class Extra {

    @NotBlank
    private final String name;

    @NotNull
    private final BigDecimal amount;
}
