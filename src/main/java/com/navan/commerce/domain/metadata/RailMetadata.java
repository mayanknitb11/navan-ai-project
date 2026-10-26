package com.navan.commerce.domain.metadata;

import com.navan.commerce.domain.metadata.support.Leg;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

/** Rail journey: a through journey with a change of train is two legs, not one OD pair. */
@Getter
@Builder
@Jacksonized
public final class RailMetadata implements TransactionMetadata {

    @NotEmpty
    private final List<@Valid Leg> legs;
}
