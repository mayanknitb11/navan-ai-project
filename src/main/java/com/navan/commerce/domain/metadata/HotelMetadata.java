package com.navan.commerce.domain.metadata;

import com.navan.commerce.domain.metadata.support.Address;
import com.navan.commerce.domain.metadata.support.Extra;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Hotel stay: property name + address, stay window, nightly rate, optional extras (e.g. breakfast). */
@Getter
@Builder
@Jacksonized
public final class HotelMetadata implements TransactionMetadata {

    @NotBlank
    private final String name;

    @NotNull
    @Valid
    private final Address address;

    @NotNull
    private final LocalDate checkIn;

    @NotNull
    private final LocalDate checkOut;

    @NotNull
    @Positive
    private final Integer nights;

    @NotNull
    private final BigDecimal roomRatePerNight;

    private final List<@Valid Extra> extras;
}
